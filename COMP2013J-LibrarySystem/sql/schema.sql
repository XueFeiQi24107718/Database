-- Library Management System - relational schema
-- Demonstrates: PK/FK, M:N junction tables, CHECK constraints, indexes

CREATE TABLE IF NOT EXISTS staff (
    staff_id     INT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(64) NOT NULL,
    full_name    VARCHAR(100) NOT NULL,
    role         VARCHAR(20)  NOT NULL DEFAULT 'LIBRARIAN',
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS member (
    member_id    INT AUTO_INCREMENT PRIMARY KEY,
    membership_no VARCHAR(20) NOT NULL UNIQUE,
    first_name   VARCHAR(50)  NOT NULL,
    last_name    VARCHAR(50)  NOT NULL,
    email        VARCHAR(100),
    phone        VARCHAR(20),
    joined_date  DATE         NOT NULL,
    status       VARCHAR(15)  NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT chk_member_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'EXPIRED'))
);

CREATE TABLE IF NOT EXISTS category (
    category_id  INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(60)  NOT NULL UNIQUE,
    description  VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS author (
    author_id    INT AUTO_INCREMENT PRIMARY KEY,
    first_name   VARCHAR(50)  NOT NULL,
    last_name    VARCHAR(50)  NOT NULL,
    birth_year   INT,
    CONSTRAINT chk_author_year CHECK (birth_year IS NULL OR birth_year BETWEEN 1000 AND 2100)
);

CREATE TABLE IF NOT EXISTS book (
    book_id      INT AUTO_INCREMENT PRIMARY KEY,
    isbn         VARCHAR(20)  NOT NULL UNIQUE,
    title        VARCHAR(200) NOT NULL,
    publisher    VARCHAR(100),
    publish_year INT,
    total_copies INT          NOT NULL DEFAULT 1,
    available_copies INT      NOT NULL DEFAULT 1,
    shelf_location VARCHAR(30),
    CONSTRAINT chk_book_copies CHECK (total_copies >= 0 AND available_copies >= 0 AND available_copies <= total_copies)
);

CREATE TABLE IF NOT EXISTS book_author (
    book_id      INT NOT NULL,
    author_id    INT NOT NULL,
    PRIMARY KEY (book_id, author_id),
    FOREIGN KEY (book_id)   REFERENCES book(book_id)   ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES author(author_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS book_category (
    book_id      INT NOT NULL,
    category_id  INT NOT NULL,
    PRIMARY KEY (book_id, category_id),
    FOREIGN KEY (book_id)     REFERENCES book(book_id)         ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES category(category_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS loan (
    loan_id      INT AUTO_INCREMENT PRIMARY KEY,
    book_id      INT NOT NULL,
    member_id    INT NOT NULL,
    staff_id     INT NOT NULL,
    loan_date    DATE NOT NULL,
    due_date     DATE NOT NULL,
    return_date  DATE,
    status       VARCHAR(15) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT chk_loan_status CHECK (status IN ('ACTIVE', 'RETURNED', 'OVERDUE')),
    CONSTRAINT chk_loan_dates CHECK (due_date >= loan_date),
    FOREIGN KEY (book_id)   REFERENCES book(book_id),
    FOREIGN KEY (member_id) REFERENCES member(member_id),
    FOREIGN KEY (staff_id)  REFERENCES staff(staff_id)
);

CREATE TABLE IF NOT EXISTS fine (
    fine_id      INT AUTO_INCREMENT PRIMARY KEY,
    loan_id      INT NOT NULL UNIQUE,
    amount       DECIMAL(8,2) NOT NULL,
    paid         BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at      TIMESTAMP,
    CONSTRAINT chk_fine_amount CHECK (amount >= 0),
    FOREIGN KEY (loan_id) REFERENCES loan(loan_id) ON DELETE CASCADE
);

CREATE INDEX idx_loan_member ON loan(member_id);
CREATE INDEX idx_loan_book ON loan(book_id);
CREATE INDEX idx_loan_status ON loan(status);
CREATE INDEX idx_book_title ON book(title);
