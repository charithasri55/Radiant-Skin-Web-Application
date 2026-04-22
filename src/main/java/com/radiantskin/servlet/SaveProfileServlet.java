package com.radiantskin.servlet;

import com.radiantskin.util.DBConnection;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;

/**
 * GET /profile - If NOT logged in → redirect to LoginPage.html - If logged in
 * but no profile saved yet → returns empty-profile HTML - If logged in and
 * profile exists → returns that user's profile HTML
 *
 * This mirrors the exact same session-guard + per-user-query pattern used in
 * OrderedProductsServlet (GET /myOrders).
 */
@WebServlet("/profile")
public class SaveProfileServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

		res.setContentType("text/html;charset=UTF-8");

		// ── 1. Session guard (same as OrderedProductsServlet) ──────────────
		HttpSession session = req.getSession(false);
		if (session == null || session.getAttribute("userId") == null) {
			res.sendRedirect("LoginPage.html?error="
					+ java.net.URLEncoder.encode("Please login to view your profile.", "UTF-8"));
			return;
		}

		int userId = (int) session.getAttribute("userId");
		String userName = (String) session.getAttribute("userName");

		// ── 2. Query only THIS user's profile ──────────────────────────────
		StringBuilder profileHtml = new StringBuilder();
		boolean hasProfile = false;

		try (Connection con = DBConnection.getConnection()) {

			PreparedStatement ps = con.prepareStatement("SELECT name, email, phone, skin_type, skin_concerns, "
					+ "       routine, lifestyle, allergies, goals " + "FROM   skin_profiles " + "WHERE  user_id = ? "
					+ "ORDER BY rowid DESC FETCH FIRST 1 ROWS ONLY" // Oracle syntax, latest entry
			);
			ps.setInt(1, userId);
			ResultSet rs = ps.executeQuery();

			if (rs.next()) {
				hasProfile = true;

				String name = esc(rs.getString("name"));
				String email = esc(rs.getString("email"));
				String phone = esc(rs.getString("phone"));
				String skinType = esc(rs.getString("skin_type"));
				String concerns = esc(rs.getString("skin_concerns"));
				String routine = esc(rs.getString("routine"));
				String lifestyle = esc(rs.getString("lifestyle"));
				String allergies = esc(rs.getString("allergies"));
				String goals = esc(rs.getString("goals"));

				// Build skincare tip based on skin type
				String tips;
				String st = skinType.toLowerCase();
				if (st.contains("oily"))
					tips = "Use an oil-free cleanser, avoid heavy creams, incorporate salicylic acid.";
				else if (st.contains("dry"))
					tips = "Use a hydrating cleanser, apply moisturiser twice daily, use hyaluronic acid.";
				else if (st.contains("sensitive"))
					tips = "Use fragrance-free products, avoid harsh exfoliants, patch-test new items.";
				else if (st.contains("combination"))
					tips = "Use a gentle balancing cleanser; moisturise dry zones, use light gel on the T-zone.";
				else
					tips = "Maintain a balanced routine with daily SPF and consistent hydration.";

				// Concern badges
				StringBuilder badges = new StringBuilder();
				if (!concerns.isEmpty()) {
					for (String c : concerns.split(",")) {
						badges.append("<span class='badge'>").append(c.trim()).append("</span>");
					}
				} else {
					badges.append("<span class='badge'>—</span>");
				}

				profileHtml.append("<div class='profile-container'>" +

				// ── LEFT PANEL ──────────────────────────────────────────
						"<div class='left-panel'>"
						+ "<img id='profileImage' src='https://via.placeholder.com/150' alt='Profile Photo'>" + "<br>"
						+ "<button class='edit-btn' onclick=\"document.getElementById('photoInput').click()\">Edit Photo</button>"
						+ "<input type='file' id='photoInput' hidden>" + "<h2>" + name + "</h2>" + "<p>" + email
						+ "</p>" + "<p>" + phone + "</p>" + "<br>"
						+ "<a href='UserInfo.html' class='edit-profile-btn'>✏️ Edit Profile</a>" + "</div>" +

						// ── RIGHT PANEL ─────────────────────────────────────────
						"<div class='right-panel'>" +

						"<div class='card'>" + "<h3>🌿 Skin Profile</h3>" + "<p><strong>Skin Type:</strong> " + skinType
						+ "</p>" + "<p><strong>Concerns:</strong> " + badges + "</p>"
						+ "<p><strong>Follows Routine:</strong> " + routine + "</p>" + "</div>" +

						"<div class='card'>" + "<h3>💡 Lifestyle &amp; Health</h3>" + "<p><strong>Lifestyle:</strong> "
						+ lifestyle + "</p>" + "<p><strong>Allergies:</strong> " + allergies + "</p>"
						+ "<p><strong>Goals:</strong> " + goals + "</p>" + "</div>" +

						"<div class='card'>" + "<h3>✨ Skincare Suggestions</h3>" + "<div class='routine-box'>" + tips
						+ "</div>" + "</div>" +

						"</div>" + // end right-panel
						"</div>" // end profile-container
				);
			}

		} catch (Exception e) {
			e.printStackTrace();
			profileHtml.append("<p style='color:red;text-align:center;padding:30px;'>Error loading profile: ")
					.append(esc(e.getMessage())).append("</p>");
		}

		// ── 3. No profile yet → empty state with prompt ────────────────────
		if (!hasProfile) {
			profileHtml.append("<div class='empty-profile'>" + "<div class='empty-icon'>🧴</div>"
					+ "<h3>No Profile Found</h3>" + "<p>Hi <strong>" + esc(userName)
					+ "</strong>! You haven't set up your skincare profile yet.</p>"
					+ "<p>Fill in your details to get personalised product recommendations.</p>"
					+ "<a href='UserInfo.html' class='create-btn'>Create My Profile</a>" + "</div>");
		}

		// ── 4. Full HTML page (same structure as OrderedProductsServlet) ───
		PrintWriter out = res.getWriter();
		out.println(buildPage(profileHtml.toString(), userName));
	}

	// ── Page builder ────────────────────────────────────────────────────────

	private String buildPage(String body, String userName) {
		return "<!DOCTYPE html>\n<html lang='en'>\n<head>\n" + "<meta charset='UTF-8'/>\n"
				+ "<meta name='viewport' content='width=device-width,initial-scale=1'/>\n"
				+ "<title>My Profile - Radiant Skin</title>\n"
				+ "<link href='https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&display=swap' rel='stylesheet'/>\n"
				+ "<style>\n"
				+ ":root{--black:#000;--white:#fff;--teal:#26a69a;--green:#00695c;--azure:#f0ffff;--gray:#6c757d;--border:#e0e0e0;}\n"
				+ "*{margin:0;padding:0;box-sizing:border-box;font-family:'Poppins',sans-serif;}\n"
				+ "body{background:linear-gradient(to right,#e0f7fa,#f9fbe7);min-height:100vh;}\n" +

				/* ── NAVBAR ── */
				".navbar{display:flex;align-items:center;justify-content:space-between;padding:10px 30px;background:var(--black);color:var(--white);position:sticky;top:0;z-index:1000;}\n"
				+ ".navbar-left{display:flex;align-items:center;gap:10px;}\n" + ".icon{width:35px;height:35px;}\n"
				+ ".logo{font-size:24px;font-weight:bold;}\n" + ".menu{display:flex;align-items:center;}\n"
				+ ".menu a{color:var(--white);text-decoration:none;margin:0 15px;font-size:18px;transition:0.3s;}\n"
				+ ".menu a:hover{color:gray;}\n" + ".dropdown{position:relative;}\n"
				+ ".dropdown-content{display:none;position:absolute;background-color:#333;min-width:160px;right:0;border-radius:5px;box-shadow:0 8px 16px rgba(0,0,0,0.2);z-index:10;}\n"
				+ ".dropdown-content a{color:var(--white);padding:10px 16px;display:block;font-size:16px;text-decoration:none;}\n"
				+ ".dropdown-content a:hover{background-color:#555;}\n"
				+ ".dropdown:hover .dropdown-content{display:block;}\n" +

				/* ── PROFILE LAYOUT ── */
				".profile-container{max-width:1100px;margin:40px auto;display:grid;grid-template-columns:1fr 2fr;gap:30px;padding:0 20px 60px;}\n"
				+ "@media(max-width:768px){.profile-container{grid-template-columns:1fr;}}\n" +

				/* ── LEFT PANEL ── */
				".left-panel{background:var(--white);padding:30px;border-radius:15px;text-align:center;box-shadow:0 10px 25px rgba(0,0,0,0.1);}\n"
				+ ".left-panel img{width:140px;height:140px;border-radius:50%;border:4px solid var(--teal);}\n"
				+ ".left-panel h2{margin-top:12px;color:#222;}\n"
				+ ".left-panel p{color:var(--gray);font-size:14px;margin-top:4px;}\n"
				+ ".edit-btn{margin-top:10px;background:var(--teal);color:var(--white);border:none;padding:8px 15px;border-radius:20px;cursor:pointer;font-size:13px;}\n"
				+ ".edit-profile-btn{display:inline-block;margin-top:16px;background:var(--black);color:var(--white);padding:9px 18px;border-radius:20px;text-decoration:none;font-size:13px;font-weight:600;transition:0.3s;}\n"
				+ ".edit-profile-btn:hover{background:#333;}\n" +

				/* ── RIGHT PANEL ── */
				".right-panel{display:grid;gap:20px;}\n"
				+ ".card{background:var(--white);padding:20px;border-radius:15px;box-shadow:0 8px 20px rgba(0,0,0,0.08);}\n"
				+ ".card h3{margin-bottom:12px;color:var(--green);}\n"
				+ ".card p{margin:7px 0;font-size:14px;color:#444;}\n"
				+ ".badge{display:inline-block;background:#e0f2f1;color:var(--green);padding:4px 11px;border-radius:12px;margin:3px;font-size:12px;font-weight:500;}\n"
				+ ".routine-box{background:#f1f8e9;padding:14px;border-radius:10px;font-size:14px;color:#444;line-height:1.6;}\n"
				+

				/* ── EMPTY STATE ── */
				".empty-profile{text-align:center;padding:80px 20px;max-width:500px;margin:auto;}\n"
				+ ".empty-icon{font-size:4rem;margin-bottom:16px;}\n"
				+ ".empty-profile h3{font-size:1.6rem;color:#333;margin-bottom:10px;}\n"
				+ ".empty-profile p{color:var(--gray);margin-bottom:8px;font-size:15px;}\n"
				+ ".create-btn{display:inline-block;margin-top:20px;background:var(--black);color:var(--white);padding:12px 28px;border-radius:8px;text-decoration:none;font-weight:600;font-size:15px;transition:0.3s;}\n"
				+ ".create-btn:hover{background:#333;}\n" + "</style>\n</head>\n<body>\n" +

				// NAVBAR
				"<div class='navbar'>\n" + "  <div class='navbar-left'>\n"
				+ "    <img src='Images/face-cream.png' alt='App Icon' class='icon'>\n"
				+ "    <div class='logo'>Radiant Skin</div>\n" + "  </div>\n" + "  <div class='menu'>\n"
				+ "    <a href='Homepage.html'>Home</a>\n" + "    <a href='products.html'>Products</a>\n"
				+ "    <div class='dropdown'>\n" + "      <a href='#'>Dashboard</a>\n"
				+ "      <div class='dropdown-content'>\n" + "        <a href='cart.html'>My Cart</a>\n"
				+ "        <a href='myorders.html'>My Orders</a>\n" + "        <a href='logout'>Logout</a>\n"
				+ "      </div>\n" + "    </div>\n" +

				"  </div>\n" + "</div>\n" +

				// BODY
				body +

				// Photo script (restore saved photo)
				"<script>\n" + "const saved=localStorage.getItem('profilePhoto');\n"
				+ "if(saved&&document.getElementById('profileImage'))document.getElementById('profileImage').src=saved;\n"
				+ "const pi=document.getElementById('photoInput');\n"
				+ "if(pi){pi.addEventListener('change',function(){\n" + "  const r=new FileReader();\n"
				+ "  r.onload=e=>{document.getElementById('profileImage').src=e.target.result;localStorage.setItem('profilePhoto',e.target.result);};\n"
				+ "  r.readAsDataURL(this.files[0]);\n" + "});}\n" + "</script>\n" + "</body>\n</html>";
	}

	private String esc(String s) {
		if (s == null)
			return "";
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}
}