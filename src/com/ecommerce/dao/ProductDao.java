package com.ecommerce.dao;

import com.ecommerce.db.DatabaseConnection;
import com.ecommerce.model.Product;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProductDao {

    private static final String BASE_SELECT = """
            SELECT p.product_id, p.product_name, p.description, p.price, p.stock_quantity, p.is_active,
                   p.category_id, c.category_name
            FROM product p
            JOIN category c ON p.category_id = c.category_id
            """;

    public List<Product> findAllForAdmin(String search, Integer categoryId, boolean includeInactive) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, search, categoryId);
        if (!includeInactive) {
            sql.append(" AND p.is_active = TRUE ");
        }
        sql.append(" ORDER BY p.product_id DESC");
        return query(sql.toString(), params);
    }

    public List<Product> findActiveForCustomer(String search, Integer categoryId) throws SQLException {
        StringBuilder sql = new StringBuilder(BASE_SELECT)
                .append(" WHERE p.is_active = TRUE AND p.stock_quantity > 0 ");
        List<Object> params = new ArrayList<>();
        appendFilters(sql, params, search, categoryId);
        sql.append(" ORDER BY p.product_id DESC");
        return query(sql.toString(), params);
    }

    private void appendFilters(StringBuilder sql, List<Object> params, String search, Integer categoryId) {
        String term = search == null ? "" : search.trim().toLowerCase();
        if (!term.isBlank()) {
            sql.append(" AND (LOWER(p.product_name) LIKE ? OR LOWER(COALESCE(p.description,'')) LIKE ?) ");
            String pattern = "%" + term + "%";
            params.add(pattern);
            params.add(pattern);
        }
        if (categoryId != null && categoryId > 0) {
            sql.append(" AND p.category_id = ? ");
            params.add(categoryId);
        }
    }

    private List<Product> query(String sql, List<Object> params) throws SQLException {
        List<Product> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public Optional<Product> findById(int id) throws SQLException {
        String sql = BASE_SELECT + " WHERE p.product_id = ?";
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public void insert(Product p) throws SQLException {
        String sql = """
                INSERT INTO product (product_name, description, price, stock_quantity, is_active, category_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, p);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    p.setProductId(keys.getInt(1));
                }
            }
        }
    }

    public void update(Product p) throws SQLException {
        String sql = """
                UPDATE product
                SET product_name=?, description=?, price=?, stock_quantity=?, is_active=?, category_id=?
                WHERE product_id=?
                """;
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getProductName());
            ps.setString(2, p.getDescription());
            ps.setBigDecimal(3, p.getPrice() == null ? BigDecimal.ZERO : p.getPrice());
            ps.setInt(4, p.getStockQuantity());
            ps.setBoolean(5, p.isActive());
            ps.setInt(6, p.getCategoryId());
            ps.setInt(7, p.getProductId());
            ps.executeUpdate();
        }
    }

    public void delete(int productId) throws SQLException {
        try (Connection conn = DatabaseConnection.open()) {
            try (PreparedStatement check = conn.prepareStatement("SELECT COUNT(*) FROM order_item WHERE product_id = ?")) {
                check.setInt(1, productId);
                try (ResultSet rs = check.executeQuery()) {
                    rs.next();
                    if (rs.getInt(1) > 0) {
                        throw new SQLException("Cannot delete product that appears in orders. You can set it inactive instead.");
                    }
                }
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM product WHERE product_id = ?")) {
                ps.setInt(1, productId);
                ps.executeUpdate();
            }
        }
    }

    private void bind(PreparedStatement ps, Product p) throws SQLException {
        ps.setString(1, p.getProductName());
        ps.setString(2, p.getDescription());
        ps.setBigDecimal(3, p.getPrice() == null ? BigDecimal.ZERO : p.getPrice());
        ps.setInt(4, p.getStockQuantity());
        ps.setBoolean(5, p.isActive());
        ps.setInt(6, p.getCategoryId());
    }

    private Product map(ResultSet rs) throws SQLException {
        Product p = new Product();
        p.setProductId(rs.getInt("product_id"));
        p.setProductName(rs.getString("product_name"));
        p.setDescription(rs.getString("description"));
        p.setPrice(rs.getBigDecimal("price"));
        p.setStockQuantity(rs.getInt("stock_quantity"));
        p.setActive(rs.getBoolean("is_active"));
        p.setCategoryId(rs.getInt("category_id"));
        p.setCategoryName(rs.getString("category_name"));
        return p;
    }
}

