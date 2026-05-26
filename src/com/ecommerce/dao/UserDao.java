package com.ecommerce.dao;

import com.ecommerce.db.DatabaseConnection;
import com.ecommerce.model.User;
import com.ecommerce.util.PasswordUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserDao {

    public Optional<User> authenticate(String username, String password) throws SQLException {
        String sql = """
                SELECT user_id, username, email, role
                FROM users
                WHERE username = ? AND password_hash = ?
                """;
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setString(2, PasswordUtil.sha256(password));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User(
                            rs.getInt("user_id"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("role"));
                    return Optional.of(u);
                }
            }
        }
        return Optional.empty();
    }
}

