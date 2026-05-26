package com.ecommerce.ui;

import com.ecommerce.dao.CategoryDao;
import com.ecommerce.model.Category;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class CategoryPanel extends JPanel {

    private final CategoryDao dao = new CategoryDao();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Name", "Description"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public CategoryPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(buildToolbar(), BorderLayout.NORTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addBtn = new JButton("Add Category");
        addBtn.addActionListener(e -> showDialog(null));
        bar.add(addBtn);
        JButton editBtn = new JButton("Edit");
        editBtn.addActionListener(e -> editSelected());
        bar.add(editBtn);
        JButton delBtn = new JButton("Delete");
        delBtn.addActionListener(e -> deleteSelected());
        bar.add(delBtn);
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());
        bar.add(refreshBtn);
        return bar;
    }

    public void refresh() {
        try {
            List<Category> list = dao.findAll();
            tableModel.setRowCount(0);
            for (Category c : list) {
                tableModel.addRow(new Object[]{c.getCategoryId(), c.getCategoryName(), c.getDescription()});
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a category first.");
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        try {
            dao.findById(id).ifPresentOrElse(this::showDialog, () ->
                    JOptionPane.showMessageDialog(this, "Category not found."));
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this, "Delete this category?", "Confirm", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            dao.delete(id);
            refresh();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showDialog(Category existing) {
        JTextField name = new JTextField(existing != null ? existing.getCategoryName() : "", 18);
        JTextField desc = new JTextField(existing != null ? existing.getDescription() : "", 18);
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("Name:"));
        panel.add(name);
        panel.add(new JLabel("Description:"));
        panel.add(desc);
        if (JOptionPane.showConfirmDialog(this, panel, existing == null ? "Add Category" : "Edit Category",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            Category c = existing != null ? existing : new Category();
            c.setCategoryName(name.getText().trim());
            c.setDescription(desc.getText().trim().isBlank() ? null : desc.getText().trim());
            if (existing == null) {
                dao.insert(c);
            } else {
                dao.update(c);
            }
            refresh();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}

