-- Sample data for demonstration and testing

INSERT INTO staff (username, password_hash, full_name, role) VALUES
('admin', '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918', 'System Administrator', 'ADMIN'),
('librarian', '6b86b273ff34fce19d6b804eff5a3f5747ada4eaa22f1d49c01e52ddb7875b4b', 'Jane Librarian', 'LIBRARIAN');

INSERT INTO category (name, description) VALUES
('Fiction', 'Novels and literary fiction'),
('Science', 'Natural and applied sciences'),
('History', 'Historical works'),
('Computing', 'Programming and computer science');

INSERT INTO author (first_name, last_name, birth_year) VALUES
('George', 'Orwell', 1903),
('Ada', 'Lovelace', 1815),
('Tim', 'Berners-Lee', 1955);

INSERT INTO book (isbn, title, publisher, publish_year, total_copies, available_copies, shelf_location) VALUES
('9780141036144', '1984', 'Penguin', 1949, 3, 3, 'A-12'),
('9780262535073', 'Introduction to Algorithms', 'MIT Press', 2009, 2, 2, 'C-04'),
('9780596009205', 'Head First Java', 'O''Reilly', 2005, 4, 4, 'C-01');

INSERT INTO book_author (book_id, author_id) VALUES (1, 1), (3, 3);
INSERT INTO book_category (book_id, category_id) VALUES
(1, 1), (2, 4), (2, 2), (3, 4);

INSERT INTO member (membership_no, first_name, last_name, email, phone, joined_date, status) VALUES
('M-1001', 'Alice', 'Murphy', 'alice@example.com', '0871111111', '2025-09-01', 'ACTIVE'),
('M-1002', 'Bob', 'Kelly', 'bob@example.com', '0872222222', '2025-10-15', 'ACTIVE'),
('M-1003', 'Carol', 'Ryan', NULL, NULL, '2024-01-20', 'SUSPENDED');
