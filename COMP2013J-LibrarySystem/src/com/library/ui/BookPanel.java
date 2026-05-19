package com.library.ui;

import com.library.dao.BookDao;
import com.library.model.Book;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class BookPanel extends JPanel {

    private final BookDao bookDao = new BookDao();
    private final JTextField searchField = new JTextField(20);
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "ISBN", "Title", "Publisher", "Year", "Available", "Total", "Shelf", "Authors", "Categories"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public BookPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(buildToolbar(), BorderLayout.NORTH);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.add(new JLabel("Search:"));
        bar.add(searchField);
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> refresh());
        bar.add(searchBtn);
        JButton addBtn = new JButton("Add Book");
        addBtn.addActionListener(e -> showBookDialog(null));
        bar.add(addBtn);
        JButton editBtn = new JButton("Edit");
        editBtn.addActionListener(e -> editSelected());
        bar.add(editBtn);
        JButton delBtn = new JButton("Delete");
        delBtn.addActionListener(e -> deleteSelected());
        bar.add(delBtn);
        return bar;
    }

    public void refresh() {
        try {
            List<Book> books = bookDao.findAll(searchField.getText());
            tableModel.setRowCount(0);
            for (Book b : books) {
                tableModel.addRow(new Object[]{
                        b.getBookId(), b.getIsbn(), b.getTitle(), b.getPublisher(),
                        b.getPublishYear(), b.getAvailableCopies(), b.getTotalCopies(),
                        b.getShelfLocation(), b.getAuthorsSummary(), b.getCategoriesSummary()
                });
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a book first.");
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        try {
            bookDao.findById(id).ifPresentOrElse(this::showBookDialog, () ->
                    JOptionPane.showMessageDialog(this, "Book not found."));
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        int ok = JOptionPane.showConfirmDialog(this, "Delete this book?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            try {
                bookDao.delete(id);
                refresh();
            } catch (SQLException ex) {
                showError(ex);
            }
        }
    }

    private void showBookDialog(Book existing) {
        JTextField isbn = new JTextField(existing != null ? existing.getIsbn() : "", 20);
        JTextField title = new JTextField(existing != null ? existing.getTitle() : "", 20);
        JTextField publisher = new JTextField(existing != null ? existing.getPublisher() : "", 20);
        JTextField year = new JTextField(existing != null && existing.getPublishYear() != null
                ? String.valueOf(existing.getPublishYear()) : "", 6);
        JTextField total = new JTextField(existing != null ? String.valueOf(existing.getTotalCopies()) : "1", 6);
        JTextField shelf = new JTextField(existing != null ? existing.getShelfLocation() : "", 10);
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("ISBN:"));
        panel.add(isbn);
        panel.add(new JLabel("Title:"));
        panel.add(title);
        panel.add(new JLabel("Publisher:"));
        panel.add(publisher);
        panel.add(new JLabel("Year:"));
        panel.add(year);
        panel.add(new JLabel("Total copies:"));
        panel.add(total);
        panel.add(new JLabel("Shelf:"));
        panel.add(shelf);
        int result = JOptionPane.showConfirmDialog(this, panel,
                existing == null ? "Add Book" : "Edit Book", JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            Book b = existing != null ? existing : new Book();
            b.setIsbn(isbn.getText().trim());
            b.setTitle(title.getText().trim());
            b.setPublisher(publisher.getText().trim());
            b.setShelfLocation(shelf.getText().trim());
            b.setPublishYear(year.getText().isBlank() ? null : Integer.parseInt(year.getText().trim()));
            b.setTotalCopies(Integer.parseInt(total.getText().trim()));
            if (existing == null) {
                b.setAvailableCopies(b.getTotalCopies());
                bookDao.insert(b);
            } else {
                bookDao.update(b);
            }
            refresh();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Invalid number in year or copies.");
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
