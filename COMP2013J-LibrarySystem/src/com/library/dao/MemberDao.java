package com.library.dao;

import com.library.db.DatabaseConnection;
import com.library.model.Member;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MemberDao {

    public List<Member> findAll(String search) throws SQLException {
        String sql = """
                SELECT member_id, membership_no, first_name, last_name, email, phone, joined_date, status
                FROM member
                WHERE (? = '' OR LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ?
                   OR LOWER(membership_no) LIKE ? OR LOWER(COALESCE(email, '')) LIKE ?)
                ORDER BY last_name, first_name
                """;
        String pattern = "%" + (search == null ? "" : search.trim().toLowerCase()) + "%";
        List<Member> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String term = search == null || search.isBlank() ? "" : search.trim().toLowerCase();
            ps.setString(1, term);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            ps.setString(5, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public Optional<Member> findById(int id) throws SQLException {
        String sql = "SELECT member_id, membership_no, first_name, last_name, email, phone, joined_date, status FROM member WHERE member_id = ?";
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

    public void insert(Member m) throws SQLException {
        String sql = """
                INSERT INTO member (membership_no, first_name, last_name, email, phone, joined_date, status)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getMembershipNo());
            ps.setString(2, m.getFirstName());
            ps.setString(3, m.getLastName());
            ps.setString(4, m.getEmail());
            ps.setString(5, m.getPhone());
            ps.setDate(6, Date.valueOf(m.getJoinedDate()));
            ps.setString(7, m.getStatus());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    m.setMemberId(keys.getInt(1));
                }
            }
        }
    }

    public void update(Member m) throws SQLException {
        String sql = """
                UPDATE member SET membership_no=?, first_name=?, last_name=?, email=?, phone=?, joined_date=?, status=?
                WHERE member_id=?
                """;
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getMembershipNo());
            ps.setString(2, m.getFirstName());
            ps.setString(3, m.getLastName());
            ps.setString(4, m.getEmail());
            ps.setString(5, m.getPhone());
            ps.setDate(6, Date.valueOf(m.getJoinedDate()));
            ps.setString(7, m.getStatus());
            ps.setInt(8, m.getMemberId());
            ps.executeUpdate();
        }
    }

    public void delete(int memberId) throws SQLException {
        try (Connection conn = DatabaseConnection.open()) {
            try (PreparedStatement check = conn.prepareStatement(
                    "SELECT COUNT(*) FROM loan WHERE member_id = ? AND status IN ('ACTIVE','OVERDUE')")) {
                check.setInt(1, memberId);
                try (ResultSet rs = check.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        throw new SQLException("Cannot delete member with active loans.");
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM member WHERE member_id = ?")) {
                ps.setInt(1, memberId);
                ps.executeUpdate();
            }
        }
    }

    private Member map(ResultSet rs) throws SQLException {
        Member m = new Member();
        m.setMemberId(rs.getInt("member_id"));
        m.setMembershipNo(rs.getString("membership_no"));
        m.setFirstName(rs.getString("first_name"));
        m.setLastName(rs.getString("last_name"));
        m.setEmail(rs.getString("email"));
        m.setPhone(rs.getString("phone"));
        Date joined = rs.getDate("joined_date");
        m.setJoinedDate(joined != null ? joined.toLocalDate() : LocalDate.now());
        m.setStatus(rs.getString("status"));
        return m;
    }
}
