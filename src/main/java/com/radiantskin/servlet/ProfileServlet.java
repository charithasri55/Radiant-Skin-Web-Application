package com.radiantskin.servlet;

import com.radiantskin.util.DBConnection;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

/**
 * POST /submit-profile - If NOT logged in → redirect to LoginPage.html - If
 * userId not in users table → redirect to LoginPage.html (stale session) -
 * Saves profile tied to the session userId (INSERT or UPDATE) - After save →
 * redirects to Profile.html
 */
@WebServlet("/submit-profile")
public class ProfileServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");

		// 1. Session guard
		HttpSession session = req.getSession(false);
		if (session == null || session.getAttribute("userId") == null) {
			res.sendRedirect("LoginPage.html?error="
					+ java.net.URLEncoder.encode("Please login to save your profile.", "UTF-8"));
			return;
		}

		// LoginServlet stores userId as Integer - cast safely
		int userId;
		try {
			userId = (Integer) session.getAttribute("userId");
		} catch (ClassCastException e) {
			session.invalidate();
			res.sendRedirect("LoginPage.html?error="
					+ java.net.URLEncoder.encode("Session corrupted. Please login again.", "UTF-8"));
			return;
		}

		// 2. Read form fields
		String name = nvl(req.getParameter("name"));
		String phone = nvl(req.getParameter("phone"));
		String email = nvl(req.getParameter("email"));
		String skinType = nvl(req.getParameter("skinType"));
		String[] ca = req.getParameterValues("skinConcerns[]");
		String concerns = ca != null ? String.join(", ", ca) : "";
		String routine = nvl(req.getParameter("routine"));
		String lifestyle = nvl(req.getParameter("lifestyle"));
		String allergies = nvl(req.getParameter("allergies"));
		String goals = nvl(req.getParameter("goals"));

		try (Connection con = DBConnection.getConnection()) {

			// 3. Verify this userId actually exists in the users table.
			// ORA-02291 = FK parent key not found, meaning skin_profiles.user_id
			// references users.user_id but that value is missing.
			// Root causes: stale session after DB reset, or session storing
			// the wrong column value.
			PreparedStatement userCheck = con.prepareStatement("SELECT user_id FROM users WHERE user_id = ?");
			userCheck.setInt(1, userId);
			ResultSet userRs = userCheck.executeQuery();
			boolean userExists = userRs.next();
			userCheck.close();

			if (!userExists) {
				session.invalidate();
				res.sendRedirect("LoginPage.html?error="
						+ java.net.URLEncoder.encode("Your account was not found. Please login again.", "UTF-8"));
				return;
			}

			// 4. UPSERT - UPDATE if profile exists, INSERT if new user
			PreparedStatement check = con.prepareStatement("SELECT user_id FROM skin_profiles WHERE user_id = ?");
			check.setInt(1, userId);
			ResultSet rs = check.executeQuery();
			boolean profileExists = rs.next();
			check.close();

			if (profileExists) {
				PreparedStatement upd = con.prepareStatement(
						"UPDATE skin_profiles " + "SET name=?, phone=?, email=?, skin_type=?, skin_concerns=?, "
								+ "    routine=?, lifestyle=?, allergies=?, goals=? " + "WHERE user_id=?");
				upd.setString(1, name);
				upd.setString(2, phone);
				upd.setString(3, email);
				upd.setString(4, skinType);
				upd.setString(5, concerns);
				upd.setString(6, routine);
				upd.setString(7, lifestyle);
				upd.setString(8, allergies);
				upd.setString(9, goals);
				upd.setInt(10, userId);
				upd.executeUpdate();
				upd.close();
			} else {
				PreparedStatement ins = con.prepareStatement(
						"INSERT INTO skin_profiles " + "(user_id, name, phone, email, skin_type, skin_concerns, "
								+ " routine, lifestyle, allergies, goals) " + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
				ins.setInt(1, userId);
				ins.setString(2, name);
				ins.setString(3, phone);
				ins.setString(4, email);
				ins.setString(5, skinType);
				ins.setString(6, concerns);
				ins.setString(7, routine);
				ins.setString(8, lifestyle);
				ins.setString(9, allergies);
				ins.setString(10, goals);
				ins.executeUpdate();
				ins.close();
			}

			res.sendRedirect("Profile.html");

		} catch (SQLException e) {
			e.printStackTrace();
			res.sendRedirect(
					"UserInfo.html?error=" + java.net.URLEncoder.encode("Database error: " + e.getMessage(), "UTF-8"));
		}
	}

	private String nvl(String s) {
		return s != null ? s.trim() : "";
	}
}