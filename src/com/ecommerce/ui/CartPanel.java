package com.ecommerce.ui;

import com.ecommerce.dao.CartDao;
import com.ecommerce.dao.OrderDao;
import com.ecommerce.model.CartItem;
import com.ecommerce.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class CartPanel extends JPanel {

    private final User user;
    private final CartDao cartDao = new CartDao();
    private final OrderDao orderDao = new OrderDao();

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"CartItemID", "Product", "Unit Price", "Qty", "Subtotal", "Stock"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JLabel totalLabel = new JLabel("Total: 0.00");

    public CartPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(buildToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        south.add(totalLabel);
        add(south, BorderLayout.SOUTH);

        refresh();
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton updateQty = new JButton("Update Qty");
        updateQty.addActionListener(e -> updateQtySelected());
        bar.add(updateQty);
        JButton remove = new JButton("Remove");
        remove.addActionListener(e -> removeSelected());
        bar.add(remove);
        JButton checkout = new JButton("Checkout");
        checkout.addActionListener(e -> checkout());
        bar.add(checkout);
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());
        bar.add(refreshBtn);
        return bar;
    }

    public void refresh() {
        try {
            List<CartItem> items = cartDao.findCartItems(user.getUserId());
            tableModel.setRowCount(0);
            BigDecimal total = BigDecimal.ZERO;
            for (CartItem ci : items) {
                total = total.add(ci.getSubtotal());
                tableModel.addRow(new Object[]{
                        ci.getCartItemId(),
                        ci.getProductName(),
                        ci.getUnitPrice(),
                        ci.getQuantity(),
                        ci.getSubtotal(),
                        ci.getStockQuantity()
                });
            }
            totalLabel.setText("Total: " + total);
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void updateQtySelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an item first.");
            return;
        }
        int cartItemId = (int) tableModel.getValueAt(row, 0);
        String current = String.valueOf(tableModel.getValueAt(row, 3));
        String qtyStr = JOptionPane.showInputDialog(this, "New quantity:", current);
        if (qtyStr == null) return;
        try {
            int qty = Integer.parseInt(qtyStr.trim());
            cartDao.updateQuantity(cartItemId, qty);
            refresh();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantity.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void removeSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int cartItemId = (int) tableModel.getValueAt(row, 0);
        try {
            cartDao.remove(cartItemId);
            refresh();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void checkout() {
        try {
            int orderId = orderDao.createOrderFromCart(user.getUserId());
            JOptionPane.showMessageDialog(this, "Order created. Order ID: " + orderId);
            refresh();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}

