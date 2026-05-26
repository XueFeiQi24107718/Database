package com.ecommerce.dao;

import com.ecommerce.db.DatabaseConnection;
import com.ecommerce.model.CartItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CartDao {

    public List<CartItem> findCartItems(int userId) throws SQLException {
        String sql = """
                SELECT ci.cart_item_id, ci.user_id, ci.product_id, ci.quantity,
                       p.product_name, p.price, p.stock_quantity
                FROM cart_item ci
                JOIN product p ON ci.product_id = p.product_id
                WHERE ci.user_id = ?
                ORDER BY ci.cart_item_id DESC
                """;
        List<CartItem> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public void addToCart(int userId, int productId, int quantityToAdd) throws SQLException {
        if (quantityToAdd <= 0) {
            throw new SQLException("Quantity must be > 0");
        }
        try (Connection conn = DatabaseConnection.open()) {
            conn.setAutoCommit(false);
            try {
                int stock;
                boolean active;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT stock_quantity, is_active FROM product WHERE product_id = ? FOR UPDATE")) {
                    ps.setInt(1, productId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Product not found.");
                        stock = rs.getInt("stock_quantity");
                        active = rs.getBoolean("is_active");
                    }
                }
                if (!active) throw new SQLException("Product is inactive.");

                int existingQty = 0;
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT quantity FROM cart_item WHERE user_id = ? AND product_id = ?")) {
                    ps.setInt(1, userId);
                    ps.setInt(2, productId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) existingQty = rs.getInt(1);
                    }
                }
                int newQty = existingQty + quantityToAdd;
                if (newQty > stock) throw new SQLException("Not enough stock. Current stock: " + stock);

                if (existingQty == 0) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO cart_item (user_id, product_id, quantity) VALUES (?, ?, ?)")) {
                        ps.setInt(1, userId);
                        ps.setInt(2, productId);
                        ps.setInt(3, newQty);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE cart_item SET quantity = ? WHERE user_id = ? AND product_id = ?")) {
                        ps.setInt(1, newQty);
                        ps.setInt(2, userId);
                        ps.setInt(3, productId);
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

    public void updateQuantity(int cartItemId, int newQty) throws SQLException {
        if (newQty <= 0) {
            throw new SQLException("Quantity must be > 0");
        }
        String sql = """
                UPDATE cart_item ci
                JOIN product p ON ci.product_id = p.product_id
                SET ci.quantity = ?
                WHERE ci.cart_item_id = ? AND ? <= p.stock_quantity
                """;
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newQty);
            ps.setInt(2, cartItemId);
            ps.setInt(3, newQty);
            int n = ps.executeUpdate();
            if (n == 0) {
                throw new SQLException("Update failed (maybe stock not enough).");
            }
        }
    }

    public void remove(int cartItemId) throws SQLException {
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM cart_item WHERE cart_item_id = ?")) {
            ps.setInt(1, cartItemId);
            ps.executeUpdate();
        }
    }

    private CartItem map(ResultSet rs) throws SQLException {
        CartItem ci = new CartItem();
        ci.setCartItemId(rs.getInt("cart_item_id"));
        ci.setUserId(rs.getInt("user_id"));
        ci.setProductId(rs.getInt("product_id"));
        ci.setQuantity(rs.getInt("quantity"));
        ci.setProductName(rs.getString("product_name"));
        ci.setUnitPrice(rs.getBigDecimal("price"));
        ci.setStockQuantity(rs.getInt("stock_quantity"));
        return ci;
    }
}

