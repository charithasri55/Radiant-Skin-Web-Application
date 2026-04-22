# 🌿 Radiant Skin Web Application

Radiant Skin is a dynamic full-stack web application that provides personalized skincare solutions based on user skin type, concerns, and lifestyle. The platform enables users to manage their profile, receive skincare recommendations, browse products, and place orders seamlessly.

---

## 🚀 Features

### 🔐 Authentication

* User Registration & Login
* Session-based authentication
* Secure user-specific data handling

### 👤 Profile Management

* Create & update personalized skincare profile
* Stores:

  * Skin type
  * Concerns
  * Lifestyle
  * Allergies
  * Goals

### 🧠 Smart Recommendations

* Personalized skincare suggestions based on profile

### 🛍️ E-commerce Functionality

* Product browsing
* Add to cart (localStorage-based)
* Order placement

### 📦 Order Management

* View order history
* Track order status
* Display ordered products dynamically

---

## 🛠️ Tech Stack

### Frontend

* HTML5
* CSS3
* JavaScript

### Backend

* Java Servlets (J2EE)
* JDBC

### Database

* Oracle / MySQL

### Tools

* Eclipse IDE
* Apache Tomcat Server
* Git & GitHub

---

## 📁 Project Structure

```
src/
Radiant-Skin-Web-Application/
│
├── src/
│   └── com/radiantskin/
│       ├── servlet/
│       │   ├── LoginServlet.java
│       |   ├── LogoutServlet.java
│       |   ├── OrderedProductsServlet.java
│       |   ├── OrderServlet.java
│       │   ├── ProfileServlet.java
│       │   ├── RegisterServlet.java
│       │   ├── SaveProfileServlet.java
│       │
│       └── util/
│           └── DBConnection.java
│
├── webapp/
│   ├── Images/
│   ├── css/
│   ├── js/
│   │   └── cart-manager.js
│   │
│   ├── cart.html
│   ├── CeraveCleanser.html
│   ├── cetaphilcleanser.html
│   ├── DeconstructCleanser.html
│   ├── DotKeyWatermelon.html
│   ├── Homepage.html
│   ├── knowaboutskin.html
│   ├── LoginPage.html
│   ├── minimalistcleanser.html
│   ├── PlumGreenTeaFaceToner.html
│   ├── SimpleCleanser.html
│   ├── reg.html
│   ├── Profile.html
│   ├── UserInfo.html
│   ├── products.html
│   ├── cart.html
│   ├── myorders.html
│   ├── orderconfirm.html
|   ├── Payment.html
│   ├── UserInfo.html
├── WEB-INF/
│   └── web.xml
│
├── database/
│   └── schema.sql
│
├── .gitignore
├── README.md
```

---

## 🔄 Application Flow

1. User registers → stored in `users` table
2. User logs in → session created
3. User creates profile → stored in `skin_profiles`
4. System provides personalized suggestions
5. User browses products & adds to cart
6. Places order → stored in `orders` & `order_items`
7. Orders displayed dynamically in "My Orders"

---

## 🗄️ Database Schema

* `users` → user credentials
* `skin_profiles` → skincare data
* `orders` → order details
* `order_items` → ordered products

---

## ⚙️ Setup Instructions

1. Clone the repository:

```
git clone https://github.com/your-username/Radiant-Skin-Web-Application.git
```

2. Import into Eclipse:

* File → Import → Existing Projects into Workspace

3. Configure Apache Tomcat

4. Setup database:

* Copy `schema.sql` commands and run in database
* Update DB credentials in `DBConnection.java`

5. Run project:

```
http://localhost:8080/Radiant-Skin-Web-Application
```

---

## 🔐 Authentication Flow

* Login creates session:

```
session.setAttribute("userId", userId);
```

* Used across:

  * Profile
  * Orders
  * Navbar

---

## 💡 Future Enhancements

* 🤖 AI-based skin analysis
* 💳 Payment gateway integration
* 📱 Fully responsive UI
* 📸 Profile image upload
* ⭐ Product ratings & reviews

---

## 👩‍💻 Author

**Charitha Sri**

---
