package com.ecommerce.dao;

import com.ecommerce.db.DatabaseConnection;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OrderDao {

    public int createOrderFromCart(int userId) throws SQLException {
        try (Connection conn = DatabaseConnection.open()) {
            conn.setAutoCommit(false);
            try {
                // 1) Read cart items + lock related products
                String cartSql = """
                        SELECT ci.product_id, ci.quantity,
                               p.product_name, p.price, p.stock_quantity, p.is_active
                        FROM cart_item ci
                        JOIN product p ON ci.product_id = p.product_id
                        WHERE ci.user_id = ?
                        FOR UPDATE
                        """;
                List<OrderItem> items = new ArrayList<>();
                BigDecimal total = BigDecimal.ZERO;
                try (PreparedStatement ps = conn.prepareStatement(cartSql)) {
                    ps.setInt(1, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            boolean active = rs.getBoolean("is_active");
                            int stock = rs.getInt("stock_quantity");
                            int qty = rs.getInt("quantity");
                            String name = rs.getString("product_name");
                            if (!active) throw new SQLException("Cart contains inactive product: " + name);
                            if (qty > stock) throw new SQLException("Not enough stock for: " + name + " (stock=" + stock + ", qty=" + qty + ")");

                            OrderItem oi = new OrderItem();
                            oi.setProductId(rs.getInt("product_id"));
                            oi.setProductName(name);
                            oi.setQuantity(qty);
                            oi.setUnitPrice(rs.getBigDecimal("price"));
                            items.add(oi);
                            total = total.add(oi.getLineTotal());
                        }
                    }
                }
                if (items.isEmpty()) throw new SQLException("Cart is empty.");

                // 2) Create order
                int orderId;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO orders (user_id, total_amount, order_status) VALUES (?, ?, 'PENDING')",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, userId);
                    ps.setBigDecimal(2, total);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("Failed to create order.");
                        orderId = keys.getInt(1);
                    }
                }

                // 3) Insert order items + decrement stock
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO order_item (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)");
                     PreparedStatement dec = conn.prepareStatement(
                             "UPDATE product SET stock_quantity = stock_quantity - ? WHERE product_id = ?")) {
                    for (OrderItem oi : items) {
                        ins.setInt(1, orderId);
                        ins.setInt(2, oi.getProductId());
                        ins.setInt(3, oi.getQuantity());
                        ins.setBigDecimal(4, oi.getUnitPrice());
                        ins.addBatch();

                        dec.setInt(1, oi.getQuantity());
                        dec.setInt(2, oi.getProductId());
                        dec.addBatch();
                    }
                    ins.executeBatch();
                    dec.executeBatch();
                }

                // 4) Clear cart
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM cart_item WHERE user_id = ?")) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                }

                conn.commit();
                return orderId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public List<Order> findOrdersForUser(int userId) throws SQLException {
        String sql = """
                SELECT o.order_id, o.user_id, u.username, o.total_amount, o.order_status, o.order_date
                FROM orders o
                JOIN users u ON o.user_id = u.user_id
                WHERE o.user_id = ?
                ORDER BY o.order_id DESC
                """;
        return queryOrders(sql, ps -> ps.setInt(1, userId));
    }

    public List<Order> findAllOrders() throws SQLException {
        String sql = """
                SELECT o.order_id, o.user_id, u.username, o.total_amount, o.order_status, o.order_date
                FROM orders o
                JOIN users u ON o.user_id = u.user_id
                ORDER BY o.order_id DESC
                """;
        return queryOrders(sql, ps -> {});
    }

    private interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private List<Order> queryOrders(String sql, Binder binder) throws SQLException {
        List<Order> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapOrder(rs));
                }
            }
        }
        return list;
    }

    public List<OrderItem> findOrderItems(int orderId) throws SQLException {
        String sql = """
                SELECT oi.order_item_id, oi.order_id, oi.product_id, p.product_name, oi.quantity, oi.unit_price
                FROM order_item oi
                JOIN product p ON oi.product_id = p.product_id
                WHERE oi.order_id = ?
                ORDER BY oi.order_item_id
                """;
        List<OrderItem> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderItem oi = new OrderItem();
                    oi.setOrderItemId(rs.getInt("order_item_id"));
                    oi.setOrderId(rs.getInt("order_id"));
                    oi.setProductId(rs.getInt("product_id"));
                    oi.setProductName(rs.getString("product_name"));
                    oi.setQuantity(rs.getInt("quantity"));
                    oi.setUnitPrice(rs.getBigDecimal("unit_price"));
                    list.add(oi);
                }
            }
        }
        return list;
    }

    public void updateOrderStatus(int orderId, String newStatus) throws SQLException {
        String sql = "UPDATE orders SET order_status = ? WHERE order_id = ?";
        try (Connection conn = DatabaseConnection.open();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus);
            ps.setInt(2, orderId);
            int n = ps.executeUpdate();
            if (n == 0) throw new SQLException("Order not found.");
        }
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order o = new Order();
        o.setOrderId(rs.getInt("order_id"));
        o.setUserId(rs.getInt("user_id"));
        o.setUsername(rs.getString("username"));
        o.setTotalAmount(rs.getBigDecimal("total_amount"));
        o.setOrderStatus(rs.getString("order_status"));
        Timestamp ts = rs.getTimestamp("order_date");
        o.setOrderDate(ts != null ? ts.toLocalDateTime() : LocalDateTime.now());
        return o;
    }
}

