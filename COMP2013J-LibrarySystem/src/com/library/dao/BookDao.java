package com.library.dao;

import com.library.db.DatabaseConnection;
import com.library.model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BookDao {

    private static final String BOOK_SELECT = """
            SELECT b.book_id, b.isbn, b.title, b.publisher, b.publish_year,
                   b.total_copies, b.available_copies, b.shelf_location,
                   COALESCE(GROUP_CONCAT(DISTINCT CONCAT(a.first_name, ' ', a.last_name)), '') AS authors,
                   COALESCE(GROUP_CONCAT(DISTINCT c.name), '') AS categories
            FROM book b
            LEFT JOIN book_author ba ON b.book_id = ba.book_id
            LEFT JOIN author a ON ba.author_id = a.author_id
            LEFT JOIN book_category bc ON b.book_id = bc.book_id
            LEFT JOIN category c ON bc.category_id = c.category_id
            """;

    public List<Book> findAll(String search) throws SQLException {
        String sql = BOOK_SELECT + """
                WHERE (? = '' OR LOWER(b.title) LIKE ? OR LOWER(b.isbn) LIKE ?)
                GROUP BY b.book_id, b.isbn, b.title, b.publisher, b.publish_year,
                         b.total_copies, b.available_copies, b.shelf_location
                ORDER BY b.title
                """;
        return queryBooks(sql, search);
    }

    public List<Book> findAvailable(String search) throws SQLException {
        String sql = BOOK_SELECT + """
                WHERE b.available_copies > 0
                  AND (? = '' OR LOWER(b.title) LIKE ? OR LOWER(b.isbn) LIKE ?)
                GROUP BY b.book_id, b.isbn, b.title, b.publisher, b.publish_year,
                         b.total_copies, b.available_copies, b.shelf_location
                ORDER BY b.title
                """;
        return queryBooks(sql, search);
    }

    private List<Book> queryBooks(String sql, String search) throws SQLException {
        String pattern = "%" + (search == null ? "" : search.trim().toLowerCase()) + "%";
        String term = search == null || search.isBlank() ? "" : search.trim().toLowerCase();
        List<Book> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, term);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public Optional<Book> findById(int id) throws SQLException {
        String sql = BOOK_SELECT + """
                 WHERE b.book_id = ?
                 GROUP BY b.book_id, b.isbn, b.title, b.publisher, b.publish_year,
                          b.total_copies, b.available_copies, b.shelf_location
                """;
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void insert(Book b) throws SQLException {
        String sql = """
                INSERT INTO book (isbn, title, publisher, publish_year, total_copies, available_copies, shelf_location)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindBook(ps, b);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    b.setBookId(keys.getInt(1));
                }
            }
        }
    }

    public void update(Book b) throws SQLException {
        String sql = """
                UPDATE book SET isbn=?, title=?, publisher=?, publish_year=?, total_copies=?, shelf_location=?
                WHERE book_id=?
                """;
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getIsbn());
            ps.setString(2, b.getTitle());
            ps.setString(3, b.getPublisher());
            if (b.getPublishYear() != null) {
                ps.setInt(4, b.getPublishYear());
            } else {
                ps.setNull(4, Types.INTEGER);
            }
            ps.setInt(5, b.getTotalCopies());
            ps.setString(6, b.getShelfLocation());
            ps.setInt(7, b.getBookId());
            ps.executeUpdate();
            try (PreparedStatement adj = conn.prepareStatement(
                    "UPDATE book SET available_copies = LEAST(available_copies, total_copies) WHERE book_id = ?")) {
                adj.setInt(1, b.getBookId());
                adj.executeUpdate();
            }
        }
    }

    public void delete(int bookId) throws SQLException {
        try (Connection conn = DatabaseConnection.open()) {
            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT COUNT(*) FROM loan WHERE book_id = ? AND status IN ('ACTIVE','OVERDUE')")) {
                check.setInt(1, bookId);
                try (ResultSet rs = check.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        throw new SQLException("Cannot delete book with active loans.");
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM book WHERE book_id = ?")) {
                ps.setInt(1, bookId);
                ps.executeUpdate();
            }
        }
    }

    private void bindBook(PreparedStatement ps, Book b) throws SQLException {
        ps.setString(1, b.getIsbn());
        ps.setString(2, b.getTitle());
        ps.setString(3, b.getPublisher());
        if (b.getPublishYear() != null) {
            ps.setInt(4, b.getPublishYear());
        } else {
            ps.setNull(4, Types.INTEGER);
        }
        ps.setInt(5, b.getTotalCopies());
        ps.setInt(6, b.getAvailableCopies());
        ps.setString(7, b.getShelfLocation());
    }

    private Book map(ResultSet rs) throws SQLException {
        Book b = new Book();
        b.setBookId(rs.getInt("book_id"));
        b.setIsbn(rs.getString("isbn"));
        b.setTitle(rs.getString("title"));
        b.setPublisher(rs.getString("publisher"));
        int year = rs.getInt("publish_year");
        b.setPublishYear(rs.wasNull() ? null : year);
        b.setTotalCopies(rs.getInt("total_copies"));
        b.setAvailableCopies(rs.getInt("available_copies"));
        b.setShelfLocation(rs.getString("shelf_location"));
        b.setAuthorsSummary(rs.getString("authors"));
        b.setCategoriesSummary(rs.getString("categories"));
        return b;
    }
}
