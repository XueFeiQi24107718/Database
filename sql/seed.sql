-- Sample data for demonstration and testing

INSERT INTO users (username, password_hash, email, role) VALUES
('admin',    '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'admin@example.com', 'ADMIN'),
('customer', 'b6c45863875e34487ca3c155ed145efe12a74581e27befec5aa661b8ee8ca6dd', 'customer@example.com', 'CUSTOMER');

INSERT INTO category (category_name, description) VALUES
('Electronics', 'Phones, computers, and accessories'),
('Books',       'Printed and digital books'),
('Home',        'Home and kitchen essentials');

INSERT INTO product (product_name, description, price, stock_quantity, is_active, category_id) VALUES
('Wireless Mouse', '2.4G wireless mouse', 19.99, 50, TRUE, 1),
('Mechanical Keyboard', 'Blue switch keyboard', 59.00, 20, TRUE, 1),
('Java Programming Book', 'Beginner to advanced Java', 35.50, 30, TRUE, 2),
('Coffee Mug', 'Ceramic mug 350ml', 9.90, 80, TRUE, 3);

