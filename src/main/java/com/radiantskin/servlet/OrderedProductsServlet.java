package com.radiantskin.servlet;

import com.radiantskin.util.DBConnection;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;

/**
 * GET /myOrders → returns the My Orders page (HTML) All order history for the
 * logged-in user with items and delivery dates.
 */
@WebServlet("/myOrders")
public class OrderedProductsServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		res.setContentType("text/html;charset=UTF-8");

		HttpSession session = req.getSession(false);
		if (session == null || session.getAttribute("userId") == null) {
			res.sendRedirect(
					"LoginPage.html?error=" + java.net.URLEncoder.encode("Please login to view your orders.", "UTF-8"));
			return;
		}

		int userId = (int) session.getAttribute("userId");
		String userName = (String) session.getAttribute("userName");

		StringBuilder ordersHtml = new StringBuilder();
		boolean hasOrders = false;

		try (Connection con = DBConnection.getConnection()) {
			// Fetch all orders for this user, newest first
			PreparedStatement oPs = con
					.prepareStatement("SELECT order_id, full_name, email, address, city, state, pin_code, "
							+ "       payment_mode, order_status, total_amount, ordered_at, estimated_delivery "
							+ "FROM orders WHERE user_id=? ORDER BY ordered_at DESC");
			oPs.setInt(1, userId);
			ResultSet oRs = oPs.executeQuery();

			SimpleDateFormat dtFmt = new SimpleDateFormat("dd MMM yyyy, hh:mm a");
			SimpleDateFormat dateFmt = new SimpleDateFormat("dd MMM yyyy");

			while (oRs.next()) {
				hasOrders = true;
				long orderId = oRs.getLong("order_id");
				String status = oRs.getString("order_status");
				double total = oRs.getDouble("total_amount");
				String ordered = dtFmt.format(oRs.getTimestamp("ordered_at"));
				String delivery = dateFmt.format(oRs.getDate("estimated_delivery"));
				String address = oRs.getString("address") + ", " + oRs.getString("city") + ", " + oRs.getString("state")
						+ " - " + oRs.getString("pin_code");
				String payMode = oRs.getString("payment_mode");
				String statusClass = "CONFIRMED".equals(status) ? "status-confirmed"
						: "DELIVERED".equals(status) ? "status-delivered" : "status-other";

				// Fetch items for this order
				PreparedStatement iPs = con.prepareStatement(
						"SELECT product_name, price, quantity, image_url FROM order_items WHERE order_id=?");
				iPs.setLong(1, orderId);
				ResultSet iRs = iPs.executeQuery();

				StringBuilder itemsHtml = new StringBuilder();
				while (iRs.next()) {
					String name = iRs.getString("product_name");
					double price = iRs.getDouble("price");
					int qty = iRs.getInt("quantity");
					String img = iRs.getString("image_url");
					if (img == null)
						img = "";

					itemsHtml.append("<div class='order-item'>").append("<img src='").append(escHtml(img))
							.append("' alt='").append(escHtml(name))
							.append("' class='item-img' onerror=\"this.src='https://via.placeholder.com/80x80?text=Product'\">")
							.append("<div class='item-info'>").append("<div class='item-name'>").append(escHtml(name))
							.append("</div>").append("<div class='item-meta'>Qty: ").append(qty)
							.append(" &nbsp;|&nbsp; ₹").append(String.format("%.2f", price))
							.append(" each &nbsp;|&nbsp; <strong>₹").append(String.format("%.2f", price * qty))
							.append("</strong>").append("</div></div></div>");
				}
				iRs.close();
				iPs.close();

				if (itemsHtml.length() == 0) {
					itemsHtml.append("<p class='no-items'>No item details recorded.</p>");
				}

				ordersHtml.append("<div class='order-card'>").append("<div class='order-header'>")
						.append("<div class='order-id-block'>").append("<span class='order-label'>Order ID</span>")
						.append("<span class='order-id'>#").append(orderId).append("</span>").append("</div>")
						.append("<span class='order-status ").append(statusClass).append("'>").append(status)
						.append("</span>").append("</div>")

						// Delivery banner
						.append("<div class='delivery-banner'>").append("<span class='delivery-icon'>🚚</span>")
						.append("<div>").append("<div class='delivery-label'>Estimated Delivery</div>")
						.append("<div class='delivery-date'>").append(delivery).append("</div>").append("</div></div>")

						// Items
						.append("<div class='items-section'>").append("<div class='section-title'>Items Ordered</div>")
						.append(itemsHtml).append("</div>")

						// Order meta
						.append("<div class='order-meta'>")
						.append("<div class='meta-row'><span class='meta-label'>📅 Ordered On</span><span class='meta-val'>")
						.append(ordered).append("</span></div>")
						.append("<div class='meta-row'><span class='meta-label'>📍 Delivery To</span><span class='meta-val'>")
						.append(escHtml(address)).append("</span></div>")
						.append("<div class='meta-row'><span class='meta-label'>💳 Payment</span><span class='meta-val'>")
						.append("credit-card".equals(payMode) ? "Credit/Debit Card" : "Cash on Delivery")
						.append("</span></div>")
						.append("<div class='meta-row total-row'><span class='meta-label'>💰 Total Amount</span><span class='meta-val total-amount'>₹")
						.append(String.format("%.2f", total)).append("</span></div>").append("</div>")

						// Timeline
						.append("<div class='timeline'>")
						.append("<div class='tl-step done'><div class='tl-dot'></div><div class='tl-text'>Order Placed</div></div>")
						.append("<div class='tl-line done'></div>")
						.append("<div class='tl-step done'><div class='tl-dot'></div><div class='tl-text'>Confirmed</div></div>")
						.append("<div class='tl-line'></div>")
						.append("<div class='tl-step'><div class='tl-dot'></div><div class='tl-text'>Shipped</div></div>")
						.append("<div class='tl-line'></div>")
						.append("<div class='tl-step'><div class='tl-dot'></div><div class='tl-text'>Out for Delivery</div></div>")
						.append("<div class='tl-line'></div>")
						.append("<div class='tl-step'><div class='tl-dot'></div><div class='tl-text'>Delivered</div></div>")
						.append("</div>")

						.append("</div>");
				// order-card

			}
			oRs.close();
			oPs.close();

		} catch (SQLException e) {
			e.printStackTrace();
			ordersHtml.append("<p class='error-msg'>Error loading orders: ").append(e.getMessage()).append("</p>");
		}

		if (!hasOrders && ordersHtml.indexOf("Error") < 0) {
			ordersHtml.append("<div class='empty-orders'>").append("<div class='empty-icon'>🛍️</div>")
					.append("<h3>No Orders Yet</h3>").append("<p>You haven't placed any orders. Start shopping!</p>")
					.append("<a href='products.html' class='shop-btn'>Browse Products</a>").append("</div>");
		}

		// ── Write full HTML page ──────────────────────────────────────
		PrintWriter out = res.getWriter();
		out.println(buildPage(userName, ordersHtml.toString()));
		/*
		 * PrintWriter out = res.getWriter();
		 * 
		 * out.println("<div class='page-header'>"); out.println("<h1>My Orders</h1>");
		 * out.println("<p>Hello, " + escHtml(userName) + "</p>");
		 * out.println("</div>");
		 * 
		 * out.println("<div class='container'>"); out.println(ordersHtml.toString());
		 * out.println("</div>");
		 */
	}

	private String buildPage(String userName, String ordersHtml) {
		return "<!DOCTYPE html>\n<html lang='en'>\n<head>\n" + "<meta charset='UTF-8'/>\n"
				+ "<meta name='viewport' content='width=device-width,initial-scale=1'/>\n"
				+ "<title>My Orders - Radiant Skin</title>\n"
				+ "<link href='https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&display=swap' rel='stylesheet'/>\n"
				+ "<style>\n"
				+ ":root{--black:#000;--white:#fff;--azure:#f0ffff;--green:#28a745;--pink:#C71585;--gray:#6c757d;--light-gray:#f8f9fa;--border:#e0e0e0;}\n"
				+ "*{margin:0;padding:0;box-sizing:border-box;font-family:'Poppins',sans-serif;}\n"
				+ "body{            background:linear-gradient(to right,#e0f7fa,#f9fbe7);min-height:100vh;}\r\n" +

				/* ── NAVBAR (matches Homepage exactly) ── */
				".navbar { display: flex; align-items: center; justify-content: space-between; padding: 10px 30px; background-color: black; color: white; position: sticky; top: 0; z-index: 1000;}\n"
				+ ".navbar-left { display: flex; align-items: center; gap: 10px; }\n"
				+ ".icon { width: 35px; height: 35px; }\n" + ".logo { font-size: 24px; font-weight: bold; }\n"
				+ ".menu { display: flex; align-items: center; }\n"
				+ ".menu a { color: white; text-decoration: none; margin: 0 15px; font-size: 18px; transition: 0.3s; }\n"
				+ ".menu a:hover { color: gray; }\n" + ".dropdown { position: relative; }\n"
				+ ".dropdown-content {display: none; position: absolute; background-color: #333;min-width: 160px; right: 0; border-radius: 5px;box-shadow: 0 8px 16px rgba(0,0,0,0.2); z-index: 10;}\n"
				+ ".dropdown-content a { color: white; padding: 10px 16px; display: block; font-size: 16px; text-decoration: none; }\n"
				+ ".dropdown-content a:hover { background-color: #555; }\n"
				+ ".dropdown:hover .dropdown-content { display: block; }\n"
				+ ".section { padding: 60px 30px; text-align: center; }\n"
				+ ".skincare-title {font-size: 3.5rem; font-weight: bold;background: black; -webkit-background-clip: text; -webkit-text-fill-color: transparent;margin-bottom: 10px;}\n"
				+

				/* ── PAGE ── */
				".page-header{background:var(--black);color:var(--white);padding:40px 30px;text-align:center;}\n"
				+ ".page-header h1{font-size:2.2rem;font-weight:700;letter-spacing:1px;}\n"
				+ ".page-header p{color:#aaa;margin-top:8px;font-size:1rem;}\n"
				+ ".container{max-width:900px;margin:30px auto;padding:0 20px 60px;}\n" +

				/* ── ORDER CARD ── */
				".order-card{background:var(--white);border-radius:16px;box-shadow:0 4px 20px rgba(0,0,0,0.09);margin-bottom:30px;overflow:hidden;border:1px solid var(--border);animation:fadeUp 0.5s ease both;}\n"
				+ "@keyframes fadeUp{from{opacity:0;transform:translateY(20px)}to{opacity:1;transform:translateY(0)}}\n"
				+

				".order-header{display:flex;justify-content:space-between;align-items:center;padding:18px 24px;background:var(--black);color:var(--white);}\n"
				+ ".order-id-block{display:flex;flex-direction:column;}\n"
				+ ".order-label{font-size:11px;color:#aaa;text-transform:uppercase;letter-spacing:1px;}\n"
				+ ".order-id{font-size:18px;font-weight:700;}\n"
				+ ".order-status{padding:6px 16px;border-radius:20px;font-size:13px;font-weight:600;}\n"
				+ ".status-confirmed{background:#28a745;color:#fff;}\n"
				+ ".status-delivered{background:#007bff;color:#fff;}\n"
				+ ".status-other{background:#6c757d;color:#fff;}\n" +

				/* delivery banner */
				".delivery-banner{display:flex;align-items:center;gap:16px;padding:16px 24px;background:linear-gradient(135deg,#e8f5e9,#f0fff4);border-bottom:1px solid #c8e6c9;}\n"
				+ ".delivery-icon{font-size:2rem;}\n"
				+ ".delivery-label{font-size:12px;color:var(--gray);text-transform:uppercase;letter-spacing:1px;}\n"
				+ ".delivery-date{font-size:1.3rem;font-weight:700;color:var(--green);}\n" +

				/* items */
				".items-section{padding:20px 24px;border-bottom:1px solid var(--border);}\n"
				+ ".section-title{font-size:13px;font-weight:600;color:var(--gray);text-transform:uppercase;letter-spacing:1px;margin-bottom:14px;}\n"
				+ ".order-item{display:flex;align-items:center;gap:16px;padding:12px 0;border-bottom:1px solid #f5f5f5;}\n"
				+ ".order-item:last-child{border-bottom:none;}\n"
				+ ".item-img{width:72px;height:72px;object-fit:cover;border-radius:10px;border:1px solid var(--border);flex-shrink:0;}\n"
				+ ".item-name{font-size:14px;font-weight:600;color:#222;margin-bottom:4px;}\n"
				+ ".item-meta{font-size:13px;color:var(--gray);}\n" + ".item-meta strong{color:var(--green);}\n" +

				/* meta */
				".order-meta{padding:20px 24px;border-bottom:1px solid var(--border);}\n"
				+ ".meta-row{display:flex;justify-content:space-between;align-items:flex-start;padding:8px 0;border-bottom:1px solid #f5f5f5;font-size:14px;}\n"
				+ ".meta-row:last-child{border-bottom:none;}\n"
				+ ".meta-label{color:var(--gray);font-weight:500;min-width:160px;}\n"
				+ ".meta-val{color:#222;text-align:right;}\n"
				+ ".total-row .meta-label,.total-row .meta-val{font-weight:700;font-size:15px;}\n"
				+ ".total-amount{color:var(--green);font-size:1.1rem;}\n" +

				/* timeline */
				".timeline{display:flex;align-items:center;padding:20px 24px;overflow-x:auto;gap:0;}\n"
				+ ".tl-step{display:flex;flex-direction:column;align-items:center;flex-shrink:0;}\n"
				+ ".tl-dot{width:14px;height:14px;border-radius:50%;background:#ddd;border:2px solid #ccc;}\n"
				+ ".tl-step.done .tl-dot{background:var(--green);border-color:var(--green);}\n"
				+ ".tl-text{font-size:11px;color:var(--gray);margin-top:6px;text-align:center;white-space:nowrap;}\n"
				+ ".tl-step.done .tl-text{color:var(--green);font-weight:600;}\n"
				+ ".tl-line{flex:1;height:2px;background:#ddd;min-width:30px;}\n"
				+ ".tl-line.done{background:var(--green);}\n" +

				/* empty / error */
				".empty-orders{text-align:center;padding:80px 20px;}\n"
				+ ".empty-icon{font-size:4rem;margin-bottom:16px;}\n"
				+ ".empty-orders h3{font-size:1.5rem;color:#333;margin-bottom:8px;}\n"
				+ ".empty-orders p{color:var(--gray);margin-bottom:24px;}\n"
				+ ".shop-btn{background:var(--black);color:var(--white);padding:12px 28px;border-radius:8px;text-decoration:none;font-weight:600;transition:0.3s;}\n"
				+ ".shop-btn:hover{background:#333;}\n" + ".error-msg{color:red;padding:20px;text-align:center;}\n"
				+ ".no-items{color:var(--gray);font-size:13px;padding:8px 0;}\n" + "</style>\n</head>\n<body>\n" +

				// NAVBAR
				"<div class='navbar'>\n" + "  <div class='navbar-left'>\n"
				+ "        <img src=\"Images/face-cream.png\" alt=\"App Icon\" class=\"icon\">\n"
				+ "    <div class='logo'>Radiant Skin</div>\n" + "</div>\n" + "  <div class='menu'>\n"
				+ "    <a href='products.html'>Products</a>\n" + "    <a href='LoginPage.html'>Login</a>\n"
				+ "    <div class='dropdown'>\n" + "      <a href='#'>Dashboard</a>\n"
				+ "      <div class='dropdown-content'>\n" + "        <a href='Homepage.html'>Home</a>\n"
				+ "        <a href='Profile.html'>Profile</a>\n" + "    <a href='cart.html'>My Cart</a>\n" +

				"        <a href='logout'>Logout</a>\n" + "      </div>\n" + "    </div>\n" + "  </div>\n" + "</div>\n"
				+

				// PAGE HEADER
				"<div class='page-header'>\n" + "  <h1>My Orders</h1>\n" + "  <p>Hello, " + escHtml(userName)
				+ " — here are all your orders</p>\n" + "</div>\n" +

				// ORDERS
				"<div class='container'>\n" + ordersHtml + "</div>\n</body>\n</html>";
	}

	private String escHtml(String s) {
		if (s == null)
			return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

}