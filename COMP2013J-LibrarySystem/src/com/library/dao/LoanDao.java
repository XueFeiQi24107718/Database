package com.library.dao;

import com.library.db.DatabaseConnection;
import com.library.model.Loan;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class LoanDao {

    private static final BigDecimal FINE_PER_DAY = new BigDecimal("0.50");

    public List<Loan> findActiveAndOverdue() throws SQLException {
        return findByStatus("ACTIVE", "OVERDUE");
    }

    public List<Loan> findAllRecent() throws SQLException {
        String sql = baseSelect() + " ORDER BY l.loan_date DESC, l.loan_id DESC LIMIT 100";
        return runList(sql);
    }

    private List<Loan> findByStatus(String... statuses) throws SQLException {
        String placeholders = String.join(",", java.util.Collections.nCopies(statuses.length, "?"));
        String sql = baseSelect() + " WHERE l.status IN (" + placeholders + ") ORDER BY l.due_date";
        List<Loan> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < statuses.length; i++) {
                ps.setString(i + 1, statuses[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    private List<Loan> runList(String sql) throws SQLException {
        List<Loan> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    private String baseSelect() {
        return """
                SELECT l.loan_id, l.book_id, l.member_id, l.loan_date, l.due_date, l.return_date, l.status,
                       b.title AS book_title,
                       CONCAT(m.first_name, ' ', m.last_name) AS member_name,
                       m.membership_no,
                       f.amount AS fine_amount, f.paid AS fine_paid
                FROM loan l
                JOIN book b ON l.book_id = b.book_id
                JOIN member m ON l.member_id = m.member_id
                LEFT JOIN fine f ON l.loan_id = f.loan_id
                """;
    }

    public void checkout(int bookId, int memberId, int staffId, LocalDate dueDate) throws SQLException {
        try (Connection conn = DatabaseConnection.open()) {
            conn.setAutoCommit(false);
            try {
                ensureMemberActive(conn, memberId);
                int updated = decrementAvailable(conn, bookId);
                if (updated == 0) {
                    throw new SQLException("No copies available for this book.");
                }
                String sql = """
                        INSERT INTO loan (book_id, member_id, staff_id, loan_date, due_date, status)
                        VALUES (?, ?, ?, CURRENT_DATE, ?, 'ACTIVE')
                        """;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, bookId);
                    ps.setInt(2, memberId);
                    ps.setInt(3, staffId);
                    ps.setDate(4, Date.valueOf(dueDate));
                    ps.executeUpdate();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void returnBook(int loanId) throws SQLException {
        try (Connection conn = DatabaseConnection.open()) {
            conn.setAutoCommit(false);
            try {
                Loan loan = findLoanForUpdate(conn, loanId);
                if (loan == null) {
                    throw new SQLException("Loan not found.");
                }
                if ("RETURNED".equals(loan.getStatus())) {
                    throw new SQLException("Book already returned.");
                }
                LocalDate today = LocalDate.now();
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE loan SET return_date = ?, status = 'RETURNED' WHERE loan_id = ?")) {
                    ps.setDate(1, Date.valueOf(today));
                    ps.setInt(2, loanId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE book SET available_copies = available_copies + 1 WHERE book_id = ?")) {
                    ps.setInt(1, loan.getBookId());
                    ps.executeUpdate();
                }
                long overdueDays = ChronoUnit.DAYS.between(loan.getDueDate(), today);
                if (overdueDays > 0) {
                    BigDecimal fine = FINE_PER_DAY.multiply(BigDecimal.valueOf(overdueDays));
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO fine (loan_id, amount, paid) VALUES (?, ?, FALSE)")) {
                        ps.setInt(1, loanId);
                        ps.setBigDecimal(2, fine);
                        ps.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void markFinePaid(int loanId) throws SQLException {
        String sql = """
                UPDATE fine SET paid = TRUE, paid_at = CURRENT_TIMESTAMP
                WHERE loan_id = ? AND paid = FALSE
                """;
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, loanId);
            int n = ps.executeUpdate();
            if (n == 0) {
                throw new SQLException("No unpaid fine for this loan.");
            }
        }
    }

    public int countOverdue() throws SQLException {
        String sql = "SELECT COUNT(*) FROM loan WHERE status = 'OVERDUE'";
        try (Connection conn = DatabaseConnection.open();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private void ensureMemberActive(Connection conn, int memberId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM member WHERE member_id = ?")) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Member not found.");
                }
                if (!"ACTIVE".equals(rs.getString("status"))) {
                    throw new SQLException("Member is not active.");
                }
            }
        }
    }

    private int decrementAvailable(Connection conn, int bookId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE book SET available_copies = available_copies - 1 WHERE book_id = ? AND available_copies > 0")) {
            ps.setInt(1, bookId);
            return ps.executeUpdate();
        }
    }

    private Loan findLoanForUpdate(Connection conn, int loanId) throws SQLException {
        String sql = "SELECT loan_id, book_id, due_date, status FROM loan WHERE loan_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, loanId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Loan l = new Loan();
                    l.setLoanId(rs.getInt("loan_id"));
                    l.setBookId(rs.getInt("book_id"));
                    Date due = rs.getDate("due_date");
                    l.setDueDate(due != null ? due.toLocalDate() : LocalDate.now());
                    l.setStatus(rs.getString("status"));
                    return l;
                }
            }
        }
        return null;
    }

    private Loan map(ResultSet rs) throws SQLException {
        Loan l = new Loan();
        l.setLoanId(rs.getInt("loan_id"));
        l.setBookId(rs.getInt("book_id"));
        l.setMemberId(rs.getInt("member_id"));
        l.setBookTitle(rs.getString("book_title"));
        l.setMemberName(rs.getString("member_name"));
        l.setMembershipNo(rs.getString("membership_no"));
        l.setLoanDate(toLocalDate(rs.getDate("loan_date")));
        l.setDueDate(toLocalDate(rs.getDate("due_date")));
        l.setReturnDate(toLocalDate(rs.getDate("return_date")));
        l.setStatus(rs.getString("status"));
        BigDecimal fine = rs.getBigDecimal("fine_amount");
        l.setFineAmount(fine);
        l.setFinePaid(rs.getBoolean("fine_paid"));
        return l;
    }

    private LocalDate toLocalDate(Date d) {
        return d == null ? null : d.toLocalDate();
    }
}
