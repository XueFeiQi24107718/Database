package com.ecommerce.ui;

import com.ecommerce.dao.CartDao;
import com.ecommerce.dao.CategoryDao;
import com.ecommerce.dao.ProductDao;
import com.ecommerce.model.Category;
import com.ecommerce.model.Product;
import com.ecommerce.model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class ProductPanel extends JPanel {

    private final User user;
    private final ProductDao productDao = new ProductDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final CartDao cartDao = new CartDao();

    private final JTextField searchField = new JTextField(18);
    private final JComboBox<CategoryOption> categoryBox = new JComboBox<>();
    private final JCheckBox includeInactive = new JCheckBox("Include inactive");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Name", "Category", "Price", "Stock", "Active", "Description"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public ProductPanel(User user) {
        this.user = user;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(buildToolbar(), BorderLayout.NORTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refreshCategories();
        refresh();
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.add(new JLabel("Search:"));
        bar.add(searchField);
        bar.add(new JLabel("Category:"));
        bar.add(categoryBox);
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> refresh());
        bar.add(searchBtn);

        if (user.isAdmin()) {
            includeInactive.addActionListener(e -> refresh());
            bar.add(includeInactive);
            JButton addBtn = new JButton("Add");
            addBtn.addActionListener(e -> showDialog(null));
            bar.add(addBtn);
            JButton editBtn = new JButton("Edit");
            editBtn.addActionListener(e -> editSelected());
            bar.add(editBtn);
            JButton delBtn = new JButton("Delete");
            delBtn.addActionListener(e -> deleteSelected());
            bar.add(delBtn);
            JButton toggleBtn = new JButton("Toggle Active");
            toggleBtn.addActionListener(e -> toggleActiveSelected());
            bar.add(toggleBtn);
        } else {
            JButton addToCart = new JButton("Add to Cart");
            addToCart.addActionListener(e -> addToCartSelected());
            bar.add(addToCart);
        }

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());
        bar.add(refreshBtn);
        return bar;
    }

    public void refreshCategories() {
        try {
            categoryBox.removeAllItems();
            categoryBox.addItem(CategoryOption.all());
            List<Category> cats = categoryDao.findAll();
            for (Category c : cats) {
                categoryBox.addItem(new CategoryOption(c.getCategoryId(), c.getCategoryName()));
            }
        } catch (SQLException ignored) {
        }
    }

    public void refresh() {
        try {
            CategoryOption opt = (CategoryOption) categoryBox.getSelectedItem();
            Integer categoryId = opt == null || opt.id == 0 ? null : opt.id;
            List<Product> list = user.isAdmin()
                    ? productDao.findAllForAdmin(searchField.getText(), categoryId, includeInactive.isSelected())
                    : productDao.findActiveForCustomer(searchField.getText(), categoryId);
            tableModel.setRowCount(0);
            for (Product p : list) {
                tableModel.addRow(new Object[]{
                        p.getProductId(),
                        p.getProductName(),
                        p.getCategoryName(),
                        p.getPrice(),
                        p.getStockQuantity(),
                        p.isActive(),
                        p.getDescription()
                });
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void addToCartSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a product first.");
            return;
        }
        int productId = (int) tableModel.getValueAt(row, 0);
        String name = String.valueOf(tableModel.getValueAt(row, 1));
        String qtyStr = JOptionPane.showInputDialog(this, "Quantity for \"" + name + "\":", "1");
        if (qtyStr == null) return;
        try {
            int qty = Integer.parseInt(qtyStr.trim());
            cartDao.addToCart(user.getUserId(), productId, qty);
            JOptionPane.showMessageDialog(this, "Added to cart.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid quantity.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a product first.");
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        try {
            productDao.findById(id).ifPresentOrElse(this::showDialog, () ->
                    JOptionPane.showMessageDialog(this, "Product not found."));
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this, "Delete this product?", "Confirm", JOptionPane.YES_NO_OPTION)
                != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            productDao.delete(id);
            refresh();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void toggleActiveSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(row, 0);
        try {
            Product p = productDao.findById(id).orElse(null);
            if (p == null) return;
            p.setActive(!p.isActive());
            productDao.update(p);
            refresh();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showDialog(Product existing) {
        try {
            List<Category> cats = categoryDao.findAll();
            if (cats.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No categories found. Create categories first.");
                return;
            }
            JTextField name = new JTextField(existing != null ? existing.getProductName() : "", 18);
            JTextField desc = new JTextField(existing != null ? existing.getDescription() : "", 18);
            JTextField price = new JTextField(existing != null && existing.getPrice() != null ? existing.getPrice().toString() : "0.00", 10);
            JTextField stock = new JTextField(existing != null ? String.valueOf(existing.getStockQuantity()) : "0", 6);
            JCheckBox active = new JCheckBox("Active", existing == null || existing.isActive());
            JComboBox<Category> catBox = new JComboBox<>(cats.toArray(new Category[0]));
            if (existing != null && existing.getCategoryId() != null) {
                for (Category c : cats) {
                    if (c.getCategoryId().equals(existing.getCategoryId())) {
                        catBox.setSelectedItem(c);
                        break;
                    }
                }
            }
            JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
            panel.add(new JLabel("Name:"));
            panel.add(name);
            panel.add(new JLabel("Description:"));
            panel.add(desc);
            panel.add(new JLabel("Price:"));
            panel.add(price);
            panel.add(new JLabel("Stock:"));
            panel.add(stock);
            panel.add(new JLabel("Category:"));
            panel.add(catBox);
            panel.add(new JLabel(""));
            panel.add(active);

            if (JOptionPane.showConfirmDialog(this, panel, existing == null ? "Add Product" : "Edit Product",
                    JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
                return;
            }

            Product p = existing != null ? existing : new Product();
            p.setProductName(name.getText().trim());
            p.setDescription(desc.getText().trim().isBlank() ? null : desc.getText().trim());
            p.setPrice(new BigDecimal(price.getText().trim()));
            p.setStockQuantity(Integer.parseInt(stock.getText().trim()));
            p.setActive(active.isSelected());
            Category selected = (Category) catBox.getSelectedItem();
            p.setCategoryId(selected.getCategoryId());

            if (existing == null) {
                productDao.insert(p);
            } else {
                productDao.update(p);
            }
            refresh();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number in price/stock.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static final class CategoryOption {
        final Integer id;
        final String name;

        private CategoryOption(Integer id, String name) {
            this.id = id;
            this.name = name;
        }

        static CategoryOption all() {
            return new CategoryOption(0, "All");
        }

        @Override
        public String toString() {
            return name;
        }
    }
}

