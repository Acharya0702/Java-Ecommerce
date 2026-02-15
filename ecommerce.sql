-- Create database
DROP DATABASE java_ecommerce_db;
CREATE DATABASE java_ecommerce_db;
USE java_ecommerce_db;


-- Users/Customers table
DROP TABLE users;
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    address VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    zip_code VARCHAR(20),
    country VARCHAR(100),
    role VARCHAR(20) DEFAULT 'CUSTOMER',
    
    -- User status fields
    currency VARCHAR(10) DEFAULT 'USD',
    preferred_language VARCHAR(10) DEFAULT 'en',
    newsletter_subscribed BOOLEAN DEFAULT FALSE,
    profile_image_url VARCHAR(255),
    email_verification_token VARCHAR(255),
    password_reset_token VARCHAR(255),
    password_reset_expiry DATETIME,
    last_login DATETIME,
    is_active BOOLEAN DEFAULT TRUE,
    is_email_verified BOOLEAN DEFAULT FALSE,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Indexes
    INDEX idx_email (email),
    INDEX idx_role (role),
    INDEX idx_is_active (is_active)
);
-- Categories table
CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    slug VARCHAR(255) UNIQUE NOT NULL,
    parent_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL
);

-- Products table
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    discount_price DECIMAL(10, 2),
    sku VARCHAR(100) UNIQUE NOT NULL,
    stock_quantity INT DEFAULT 0,
    category_id BIGINT,
    image_url VARCHAR(500),
    additional_images JSON,
    specifications JSON,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- Cart table
DROP TABLE cart;
CREATE TABLE cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10,2) DEFAULT 0.00,
    total_items INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Cart items table
DROP TABLE cart_items;
CREATE TABLE cart_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cart_id) REFERENCES cart(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    UNIQUE KEY unique_cart_product (cart_id, product_id)
);

-- Drop existing tables if they exist
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS order_status;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS review_votes;
DROP TABLE IF EXISTS review_images;
-- Create orders table matching the Java entity
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    subtotal DECIMAL(10, 2) DEFAULT 0.00,
    tax_amount DECIMAL(10, 2) DEFAULT 0.00,
    shipping_amount DECIMAL(10, 2) DEFAULT 0.00,
    discount_amount DECIMAL(10, 2) DEFAULT 0.00,
    
    -- Shipping address columns
    shipping_street VARCHAR(255),
    shipping_city VARCHAR(100),
    shipping_state VARCHAR(50),
    shipping_zip_code VARCHAR(20),
    shipping_country VARCHAR(100),
    shipping_phone VARCHAR(20),
    shipping_recipient_name VARCHAR(255),
    
    -- Billing address columns
    billing_street VARCHAR(255),
    billing_city VARCHAR(100),
    billing_state VARCHAR(50),
    billing_zip_code VARCHAR(20),
    billing_country VARCHAR(100),
    billing_phone VARCHAR(20),
    billing_recipient_name VARCHAR(255),
    
    -- Status and payment
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(20),
    payment_status VARCHAR(20) DEFAULT 'PENDING',
    payment_id VARCHAR(100),
    tracking_number VARCHAR(100),
    shipping_method VARCHAR(100),
    notes TEXT,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    shipped_at TIMESTAMP NULL,
    delivered_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    
    -- Foreign key
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- Indexes
    INDEX idx_order_number (order_number),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
);

-- Create order_items table matching the Java entity
CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    product_name VARCHAR(255),
    product_image_url VARCHAR(500),
    product_sku VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign keys
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id),
    
    -- Indexes
    INDEX idx_order_id (order_id),
    INDEX idx_product_id (product_id)
);


-- Reviews table
DROP TABLE reviews;
SELECT * FROM reviews;
CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

DROP TABLE review_votes;
SELECT * FROM review_votes;
CREATE TABLE review_votes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    vote_type ENUM('HELPFUL', 'UNHELPFUL') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_review_vote (user_id, review_id),
    FOREIGN KEY (review_id) REFERENCES reviews(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

DROP TABLE order_status_history;
CREATE TABLE order_status_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    notes TEXT,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Insert sample data
INSERT INTO categories (name, description, slug, parent_id) 
VALUES 
('Electronics', 'Electronic devices', 'electronics',  1),
('Clothing', 'Fashion and clothing', 'clothing',  2),
('Books', 'Books and magazines', 'books',  3);


INSERT INTO products (name, description, price, discount_price, sku, stock_quantity, category_id, image_url, is_active) 
VALUES 
('Laptop', 'High performance laptop with 16GB RAM and 512GB SSD', 999.99, 899.99, 'LAP-001', 10, 1, 'https://picsum.photos/300/200?random=1', true),
('Smartphone', 'Latest smartphone with 128GB storage', 699.99, NULL, 'PHN-001', 20, 1, 'https://picsum.photos/300/200?random=2', true),
('Headphones', 'Noise cancelling headphones', 199.99, 149.99, 'HP-001', 15, 1, 'https://picsum.photos/300/200?random=3', true),
('T-Shirt', 'Cotton t-shirt with logo', 29.99, NULL, 'TSH-001', 50, 2, 'https://picsum.photos/300/200?random=4', true),
('Jeans', 'Blue denim jeans', 59.99, 49.99, 'JNS-001', 30, 2, 'https://picsum.photos/300/200?random=5', true),
('Novel', 'Bestselling fiction novel', 14.99, NULL, 'BOK-001', 100, 3, 'https://picsum.photos/300/200?random=6', true),
('Monitor', '27-inch 4K monitor', 299.99, 249.99, 'MON-001', 8, 1, 'https://picsum.photos/300/200?random=7', true),
-- ('Chadi', '36-inch stretchable panty', 299.99, 249.99, 'PAN-001', 8, 2, 'https://images.meesho.com/images/products/395484278/wcpro_512.webp?width=512', true),
('Keyboard', 'Mechanical keyboard', 89.99, NULL, 'KB-001', 25, 1, 'https://picsum.photos/300/200?random=8', true);


SHOW TABLES;

SHOW CREATE TABLE cart;
SHOW CREATE TABLE cart_items;

SELECT * FROM users;
SELECT email_verification_token FROM users WHERE email = 'gaureeshankaracharya@gmail.com';
DELETE FROM users WHERE id = 1;
ALTER TABLE users AUTO_INCREMENT=1;

UPDATE users 
SET is_email_verified = 1, 
    email_verification_token = NULL 
WHERE email = 'gaureeshankaracharya@gmail.com';

DELETE FROM users WHERE id=3;
SELECT * FROM products;
SELECT * FROM categories;
SELECT * FROM cart;
SELECT * FROM cart_items;
DELETE FROM cart WHERE id<10;
ALTER TABLE cart AUTO_INCREMENT=1;
DELETE FROM cart_items WHERE id<5;
ALTER TABLE cart_items AUTO_INCREMENT=1;
ALTER TABLE products AUTO_INCREMENT=1;
DELETE FROM products WHERE id<5;
DELETE FROM categories WHERE id<5;
ALTER TABLE categories AUTO_INCREMENT=1;
DELETE FROM orders WHERE id<12;
ALTER TABLE orders AUTO_INCREMENT=1;
SELECT * FROM orders;
SELECT * FROM order_items;

SELECT * FROM order_items WHERE order_id = 1;
DESC users;
DESC categories;
DESC orders;
DESC order_items;


ALTER TABLE cart DROP FOREIGN KEY cart_ibfk_1;
ALTER TABLE orders DROP FOREIGN KEY orders_ibfk_1;
ALTER TABLE reviews DROP FOREIGN KEY reviews_ibfk_2;
ALTER TABLE review_votes DROP FOREIGN KEY review_votes_ibfk_2;
ALTER TABLE order_status_history DROP FOREIGN KEY order_status_history_ibfk_2;

ALTER TABLE cart ADD CONSTRAINT fk_cart_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE orders ADD CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE reviews ADD CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE review_votes ADD CONSTRAINT fk_review_votes_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE order_status_history ADD CONSTRAINT fk_order_status_history_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;



-- Check user email in database
SELECT id, email, first_name, last_name FROM users WHERE id = 1;