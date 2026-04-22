package com.radiantskin.servlet;

import com.radiantskin.util.DBConnection;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@WebServlet("/placeOrder")
public class OrderServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");

		// Billing fields
		String fullName = nvl(req.getParameter("firstname"));
		String email = nvl(req.getParameter("email"));
		String address = nvl(req.getParameter("address"));
		String city = nvl(req.getParameter("city"));
		String state = nvl(req.getParameter("state"));
		String pinCode = nvl(req.getParameter("zip"));
		String payment = nvl(req.getParameter("payment"));

		// Card details
		String cardName = nvl(req.getParameter("cardname"));
		String cardNumber = nvl(req.getParameter("cardnumber"));
		String expMonth = nvl(req.getParameter("expmonth"));
		String expYear = nvl(req.getParameter("expyear"));
		String cvv = nvl(req.getParameter("cvv"));

		// Cart JSON from hidden field (sent by Payment.html JS)
		String cartJson = nvl(req.getParameter("cartJson"));
		String totalStr = nvl(req.getParameter("totalAmount"));

		// Validation
		if (isEmpty(fullName) || isEmpty(email) || isEmpty(address) || isEmpty(city) || isEmpty(state)
				|| isEmpty(pinCode)) {
			err(res, "Please fill in all billing details.");
			return;
		}
		if (isEmpty(payment)) {
			err(res, "Please select a payment method.");
			return;
		}
		if ("credit-card".equals(payment) && (isEmpty(cardName) || isEmpty(cardNumber) || isEmpty(expMonth)
				|| isEmpty(expYear) || isEmpty(cvv))) {
			err(res, "Please fill in all card details.");
			return;
		}

		double total = 0;
		try {
			total = Double.parseDouble(totalStr);
		} catch (Exception ignored) {
		}

		// Estimated delivery = ordered_at + 5 days
		LocalDate deliveryDate = LocalDate.now().plusDays(5);

		HttpSession session = req.getSession(false);
		Integer userId = (session != null) ? (Integer) session.getAttribute("userId") : null;

		Connection con = null;
		try {
			con = DBConnection.getConnection();

			// ── Insert ORDER ──────────────────────────────────────────
			PreparedStatement oPst = con.prepareStatement(
					"INSERT INTO orders(user_id,full_name,email,address,city,state,pin_code,"
							+ "payment_mode,card_name,card_number,exp_month,exp_year,cvv,"
							+ "total_amount,estimated_delivery) " + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
					new String[] { "ORDER_ID" });

			if (userId != null)
				oPst.setInt(1, userId);
			else
				oPst.setNull(1, Types.INTEGER);
			oPst.setString(2, fullName);
			oPst.setString(3, email);
			oPst.setString(4, address);
			oPst.setString(5, city);
			oPst.setString(6, state);
			oPst.setString(7, pinCode);
			oPst.setString(8, payment);
			oPst.setString(9, cardName.isEmpty() ? null : cardName);
			oPst.setString(10, cardNumber.isEmpty() ? null : cardNumber);
			oPst.setString(11, expMonth.isEmpty() ? null : expMonth);
			oPst.setString(12, expYear.isEmpty() ? null : expYear);
			oPst.setString(13, cvv.isEmpty() ? null : cvv);
			oPst.setDouble(14, total);
			oPst.setDate(15, Date.valueOf(deliveryDate));
			oPst.executeUpdate();

			ResultSet genKeys = oPst.getGeneratedKeys();
			long orderId = 0;
			if (genKeys.next())
				orderId = genKeys.getLong(1);
			oPst.close();

			// ── Insert ORDER_ITEMS from cartJson ──────────────────────
			// cartJson format:
			// [{"id":"...","name":"...","price":350,"quantity":2,"image":"..."},...]
			if (!cartJson.isEmpty() && orderId > 0) {
				insertCartItems(con, orderId, cartJson);
			}

			// con.commit();

			// Store order info in session for confirmation page
			if (session != null) {
				session.setAttribute("lastOrderId", orderId);
				session.setAttribute("lastOrderTotal", total);
				session.setAttribute("lastDeliveryDate",
						deliveryDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
				session.setAttribute("lastOrderName", fullName);
				session.setAttribute("lastOrderEmail", email);
				session.setAttribute("lastOrderAddress", address + ", " + city + ", " + state + " - " + pinCode);
				session.setAttribute("lastOrderPayment", payment);
				session.setAttribute("lastCartJson", cartJson);
			}

			res.sendRedirect("orderconfirm.html");

		} catch (SQLException e) {
			e.printStackTrace();
			if (con != null)
				try {
					con.rollback();
				} catch (Exception ignored) {
				}
			err(res, "Database error: " + e.getMessage());
		} finally {
			if (con != null)
				try {
					con.close();
				} catch (Exception ignored) {
				}
		}
	}

	/**
	 * Parses a simple JSON array manually (no external library needed). Handles:
	 * [{"id":"x","name":"y","price":100,"quantity":2,"image":"z"}]
	 */
	private void insertCartItems(Connection con, long orderId, String cartJson) throws SQLException {
		// Minimal JSON array parser: split on },{
		String stripped = cartJson.trim();
		if (stripped.startsWith("["))
			stripped = stripped.substring(1);
		if (stripped.endsWith("]"))
			stripped = stripped.substring(0, stripped.length() - 1);
		if (stripped.isEmpty())
			return;

		String[] items = stripped.split("\\},\\s*\\{");

		PreparedStatement iPs = con
				.prepareStatement("INSERT INTO order_items(order_id,product_id,product_name,price,quantity,image_url) "
						+ "VALUES(?,?,?,?,?,?)");

		for (String item : items) {
			item = item.replace("{", "").replace("}", "");
			String pid = extractJson(item, "id");
			String pname = extractJson(item, "name");
			String img = extractJson(item, "image");
			double price = 0;
			int qty = 1;
			try {
				price = Double.parseDouble(extractJson(item, "price"));
			} catch (Exception ignored) {
			}
			try {
				qty = Integer.parseInt(extractJson(item, "quantity"));
			} catch (Exception ignored) {
			}

			iPs.setLong(1, orderId);
			iPs.setString(2, pid);
			iPs.setString(3, pname);
			iPs.setDouble(4, price);
			iPs.setInt(5, qty);
			iPs.setString(6, img);
			iPs.addBatch();
		}
		iPs.executeBatch();
		iPs.close();
	}

	/** Extracts value for a given key from a partial JSON string */
	private String extractJson(String json, String key) {
		String search = "\"" + key + "\"";
		int idx = json.indexOf(search);
		if (idx < 0)
			return "";
		int colon = json.indexOf(":", idx);
		if (colon < 0)
			return "";
		String rest = json.substring(colon + 1).trim();
		if (rest.startsWith("\"")) {
			int end = rest.indexOf("\"", 1);
			return end > 0 ? rest.substring(1, end) : "";
		} else {
			int end = rest.indexOf(",");
			if (end < 0)
				end = rest.length();
			return rest.substring(0, end).trim();
		}
	}

	private boolean isEmpty(String s) {
		return s == null || s.trim().isEmpty();
	}

	private String nvl(String s) {
		return s != null ? s.trim() : "";
	}

	private void err(HttpServletResponse res, String msg) throws IOException {
		res.sendRedirect("Payment.html?error=" + java.net.URLEncoder.encode(msg, "UTF-8"));
	}
}