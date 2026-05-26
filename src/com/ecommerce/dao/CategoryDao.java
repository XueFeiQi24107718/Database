package com.ecommerce.dao;

import com.ecommerce.db.DatabaseConnection;
import com.ecommerce.model.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CategoryDao {

    public List<Category> findAll() throws SQLException {
        String sql = "SELECT category_id, category_name, description FROM category ORDER BY category_name";
        List<Category> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    public Optional<Category> findById(int id) throws SQLException {
        String sql = "SELECT category_id, category_name, description FROM category WHERE category_id = ?";
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public void insert(Category c) throws SQLException {
        String sql = "INSERT INTO category (category_name, description) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getCategoryName());
            ps.setString(2, c.getDescription());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    c.setCategoryId(keys.getInt(1));
                }
            }
        }
    }

    public void update(Category c) throws SQLException {
        String sql = "UPDATE category SET category_name=?, description=? WHERE category_id=?";
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.getCategoryName());
            ps.setString(2, c.getDescription());
            ps.setInt(3, c.getCategoryId());
            ps.executeUpdate();
        }
    }

    public void delete(int categoryId) throws SQLException {
        try (Connection conn = DatabaseConnection.open()) {
            try (PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM product WHERE category_id = ?")) {
                check.setInt(1, categoryId);
                try (ResultSet rs = check.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        throw new SQLException("Cannot delete category that has products.");
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM category WHERE category_id = ?")) {
                ps.setInt(1, categoryId);
                ps.executeUpdate();
            }
        }
    }

    private Category map(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setCategoryId(rs.getInt("category_id"));
        c.setCategoryName(rs.getString("category_name"));
        c.setDescription(rs.getString("description"));
        return c;
    }
}

