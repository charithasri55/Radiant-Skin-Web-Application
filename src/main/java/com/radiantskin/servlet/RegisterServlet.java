package com.radiantskin.servlet;

import com.radiantskin.util.DBConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

/**
 * Handles POST from reg.html Form fields: name, email, password Success →
 * LoginPage.html Failure → reg.html with error message
 */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		request.setCharacterEncoding("UTF-8");

		String name = request.getParameter("name");
		String email = request.getParameter("email");
		String password = request.getParameter("password");

		// Basic server-side validation
		if (name == null || name.trim().isEmpty() || email == null || email.trim().isEmpty() || password == null
				|| password.trim().isEmpty()) {

			sendError(response, "reg.html", "All fields are required.");
			return;
		}

		if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$")) {
			sendError(response, "reg.html", "Invalid email format.");
			return;
		}

		Connection con = null;
		PreparedStatement checkStmt = null;
		PreparedStatement insertStmt = null;
		ResultSet rs = null;

		try {
			con = DBConnection.getConnection();

			// Check if email already exists
			checkStmt = con.prepareStatement("SELECT email FROM users WHERE email = ?");
			checkStmt.setString(1, email.trim());
			rs = checkStmt.executeQuery();

			if (rs.next()) {
				sendError(response, "reg.html", "Email already registered. Please login.");
				return;
			}

			// Insert new user (plain password – for production use hashing)
			insertStmt = con.prepareStatement("INSERT INTO users (name, email, password) VALUES (?, ?, ?)");
			insertStmt.setString(1, name.trim());
			insertStmt.setString(2, email.trim());
			insertStmt.setString(3, password); // TODO: use BCrypt in production
			insertStmt.executeUpdate();

			// con.commit();

			// Redirect to login page with success flag
			response.sendRedirect("LoginPage.html?registered=true");

		} catch (SQLException e) {
			e.printStackTrace();
			sendError(response, "reg.html", "Database error: " + e.getMessage());
		} finally {
			close(rs, checkStmt, insertStmt, con);
		}
	}

	// ── helpers ─────────────────────────────────────────────────────────────

	private void sendError(HttpServletResponse response, String page, String msg) throws IOException {
		response.sendRedirect(page + "?error=" + java.net.URLEncoder.encode(msg, "UTF-8"));
	}

	private void close(AutoCloseable... resources) {
		for (AutoCloseable r : resources) {
			if (r != null) {
				try {
					r.close();
				} catch (Exception ignored) {
				}
			}
		}
	}
}