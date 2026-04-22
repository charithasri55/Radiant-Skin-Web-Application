// cart-manager.js
class CartManager {
	static init(productId) {
		this.productId = productId;
		this.checkCartStatus();
		this.setupListeners();
	}

	static checkCartStatus() {
		const cart = JSON.parse(localStorage.getItem('cart')) || [];
		const inCart = cart.some(item => item.id === this.productId);

		const addButton = document.querySelector('.add-to-cart');
		const viewCartBtn = document.getElementById('view-cart-btn');

		if (addButton) {
			addButton.textContent = inCart ? 'Added to Cart' : 'Add to Cart';
			addButton.classList.toggle('added', inCart);
		}

		if (viewCartBtn) {
			viewCartBtn.style.display = cart.length > 0 ? 'block' : 'none';
		}
	}

	static setupListeners() {
		window.addEventListener('storage', () => this.checkCartStatus());

		window.addEventListener('productRemoved', (e) => {
			if (e.detail.productId === this.productId) {
				this.checkCartStatus();
			}
		});
	}

	static addToCart(productData) {
		let cart = JSON.parse(localStorage.getItem('cart')) || [];
		const existingIndex = cart.findIndex(item => item.id === productData.id);

		if (existingIndex >= 0) {
			cart[existingIndex].quantity += 1;
		}
		else {
			cart.push(Object.assign({}, productData, { quantity: 1 }));
		}

		localStorage.setItem('cart', JSON.stringify(cart));
		window.dispatchEvent(new Event('storage'));
		this.checkCartStatus();
	}
}