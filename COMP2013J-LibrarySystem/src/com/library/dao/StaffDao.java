package com.library.dao;

import com.library.db.DatabaseConnection;
import com.library.model.Staff;
import com.library.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class StaffDao {

    public Optional<Staff> authenticate(String username, String password) throws SQLException {
        String sql = "SELECT staff_id, username, full_name, role FROM staff WHERE username = ? AND password_hash = ?";
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setString(2, PasswordUtil.sha256(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        }
        return Optional.empty();
    }

    private Staff map(ResultSet rs) throws SQLException {
        return new Staff(
                rs.getInt("staff_id"),
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getString("role"));
    }
}
