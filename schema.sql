-- ============================================================
-- Radiant Skin - Oracle SQL Plus Schema
-- Run this FIRST in SQL Plus before starting the app
-- ============================================================

-- Drop existing tables cleanly
BEGIN EXECUTE IMMEDIATE 'DROP TABLE order_items CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE orders CASCADE CONSTRAINTS';      EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE skin_profiles CASCADE CONSTRAINTS'; EXCEPTION WHEN OTHERS THEN NULL; END;
/
BEGIN EXECUTE IMMEDIATE 'DROP TABLE users CASCADE CONSTRAINTS';       EXCEPTION WHEN OTHERS THEN NULL; END;
/

-- ── USERS ────────────────────────────────────────────────────
CREATE TABLE users (
    user_id     NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR2(100)  NOT NULL,
    email       VARCHAR2(150)  NOT NULL UNIQUE,
    password    VARCHAR2(255)  NOT NULL,
    created_at  TIMESTAMP      DEFAULT CURRENT_TIMESTAMP
);

-- ── SKIN_PROFILES ─────────────────────────────────────────────
CREATE TABLE skin_profiles (
    profile_id      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         NUMBER,
    name            VARCHAR2(100),
    phone           VARCHAR2(20),
    email           VARCHAR2(150),
    skin_type       VARCHAR2(50),
    skin_concerns   VARCHAR2(500),
    routine         VARCHAR2(10),
    lifestyle       VARCHAR2(1000),
    allergies       VARCHAR2(500),
    goals           VARCHAR2(500),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_profile_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- ── ORDERS ────────────────────────────────────────────────────
CREATE TABLE orders (
    order_id            NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             NUMBER,
    full_name           VARCHAR2(100)  NOT NULL,
    email               VARCHAR2(150)  NOT NULL,
    address             VARCHAR2(255)  NOT NULL,
    city                VARCHAR2(100)  NOT NULL,
    state               VARCHAR2(100)  NOT NULL,
    pin_code            VARCHAR2(20)   NOT NULL,
    payment_mode        VARCHAR2(30)   NOT NULL,
    card_name           VARCHAR2(100),
    card_number         VARCHAR2(20),
    exp_month           VARCHAR2(20),
    exp_year            VARCHAR2(10),
    cvv                 VARCHAR2(10),
    order_status        VARCHAR2(30)   DEFAULT 'CONFIRMED',
    total_amount        NUMBER(10,2)   DEFAULT 0,
    ordered_at          TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    estimated_delivery  DATE           DEFAULT TRUNC(SYSDATE) + 5,
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- ── ORDER_ITEMS ───────────────────────────────────────────────
CREATE TABLE order_items (
    item_id      NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id     NUMBER NOT NULL,
    product_id   VARCHAR2(200),
    product_name VARCHAR2(300) NOT NULL,
    price        NUMBER(10,2)  NOT NULL,
    quantity     NUMBER        DEFAULT 1,
    image_url    VARCHAR2(500),
    CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- Verify
SELECT table_name FROM user_tables
WHERE table_name IN ('USERS','SKIN_PROFILES','ORDERS','ORDER_ITEMS')
ORDER BY table_name;

commit;


