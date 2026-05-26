package com.ecommerce.ui;

import com.ecommerce.dao.OrderDao;
import com.ecommerce.model.Order;
import com.ecommerce.model.OrderItem;
import com.ecommerce.model.User;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class OrdersPanel extends JPanel {

    private final User user;
    private final OrderDao orderDao = new OrderDao();

    private final DefaultTableModel ordersModel = new DefaultTableModel(
            new String[]{"Order ID", "User", "Total", "Status", "Date"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable ordersTable = new JTable(ordersModel);

    private final DefaultTableModel itemsModel = new DefaultTableModel(
            new String[]{"Product", "Qty", "Unit Price", "Line Total"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable itemsTable = new JTable(itemsModel);

    public OrdersPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(buildToolbar(), BorderLayout.NORTH);

        ordersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ordersTable.getSelectionModel().addListSelectionListener(this::onOrderSelected);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(ordersTable), new JScrollPane(itemsTable));
        split.setResizeWeight(0.55);
        add(split, BorderLayout.CENTER);

        refresh();
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());
        bar.add(refreshBtn);

        if (user.isAdmin()) {
            JButton updateStatus = new JButton("Update Status");
            updateStatus.addActionListener(e -> updateStatusSelected());
            bar.add(updateStatus);
        }
        return bar;
    }

    public void refresh() {
        try {
            List<Order> orders = user.isAdmin()
                    ? orderDao.findAllOrders()
                    : orderDao.findOrdersForUser(user.getUserId());
            ordersModel.setRowCount(0);
            itemsModel.setRowCount(0);
            for (Order o : orders) {
                ordersModel.addRow(new Object[]{
                        o.getOrderId(),
                        o.getUsername(),
                        o.getTotalAmount(),
                        o.getOrderStatus(),
                        o.getOrderDate()
                });
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void onOrderSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;
        int row = ordersTable.getSelectedRow();
        if (row < 0) return;
        int orderId = (int) ordersModel.getValueAt(row, 0);
        try {
            List<OrderItem> items = orderDao.findOrderItems(orderId);
            itemsModel.setRowCount(0);
            for (OrderItem oi : items) {
                itemsModel.addRow(new Object[]{
                        oi.getProductName(),
                        oi.getQuantity(),
                        oi.getUnitPrice(),
                        oi.getLineTotal()
                });
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void updateStatusSelected() {
        int row = ordersTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select an order first.");
            return;
        }
        int orderId = (int) ordersModel.getValueAt(row, 0);
        String current = String.valueOf(ordersModel.getValueAt(row, 3));
        JComboBox<String> box = new JComboBox<>(new String[]{"PENDING", "PAID", "SHIPPED", "COMPLETED", "CANCELLED"});
        box.setSelectedItem(current);
        if (JOptionPane.showConfirmDialog(this, box, "New status", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }
        String status = (String) box.getSelectedItem();
        try {
            orderDao.updateOrderStatus(orderId, status);
            refresh();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}

