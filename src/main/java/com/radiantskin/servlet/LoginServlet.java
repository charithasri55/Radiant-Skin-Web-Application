package com.radiantskin.servlet;

import com.radiantskin.util.DBConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;

/**
 * Handles POST from LoginPage.html
 * Form fields: email, password
 * Success → Homepage.html (session created)
 * Failure → LoginPage.html with error
 */

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String email    = request.getParameter("email");
        String password = request.getParameter("password");

        if (email == null || email.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            sendError(response, "LoginPage.html", "Email and password are required.");
            return;
        }

        Connection con = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            con = DBConnection.getConnection();

            stmt = con.prepareStatement(
                "SELECT user_id, name, email FROM users WHERE email = ? AND password = ?");
            stmt.setString(1, email.trim());
            stmt.setString(2, password);
            rs = stmt.executeQuery();

            if (rs.next()) {
                // Create session
                HttpSession session = request.getSession(true);
                session.setAttribute("userId",    rs.getInt("user_id"));
                session.setAttribute("userName",  rs.getString("name"));
                session.setAttribute("userEmail", rs.getString("email"));
                session.setMaxInactiveInterval(30 * 60); // 30 minutes

                response.sendRedirect("Homepage.html");
            } else {
                sendError(response, "LoginPage.html", "Invalid email or password.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            sendError(response, "LoginPage.html", "Database error: " + e.getMessage());
        } finally {
            close(rs, stmt, con);
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private void sendError(HttpServletResponse response, String page, String msg)
            throws IOException {
        response.sendRedirect(page + "?error=" +
                java.net.URLEncoder.encode(msg, "UTF-8"));
    }

    private void close(AutoCloseable... resources) {
        for (AutoCloseable r : resources) {
            if (r != null) {
                try { r.close(); } catch (Exception ignored) {}
            }
        }
    }
}