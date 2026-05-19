package com.library.ui;

import com.library.dao.BookDao;
import com.library.dao.LoanDao;
import com.library.dao.MemberDao;
import com.library.model.Book;
import com.library.model.Loan;
import com.library.model.Member;
import com.library.model.Staff;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class LoanPanel extends JPanel {

    private final Staff staff;
    private final LoanDao loanDao = new LoanDao();
    private final BookDao bookDao = new BookDao();
    private final MemberDao memberDao = new MemberDao();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Loan ID", "Book", "Member", "Membership", "Loan Date", "Due", "Return", "Status", "Fine", "Paid"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public LoanPanel(Staff staff) {
        this.staff = staff;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(buildToolbar(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton checkout = new JButton("Check Out Book");
        checkout.addActionListener(e -> checkoutDialog());
        bar.add(checkout);
        JButton ret = new JButton("Return Selected");
        ret.addActionListener(e -> returnSelected());
        bar.add(ret);
        JButton payFine = new JButton("Mark Fine Paid");
        payFine.addActionListener(e -> payFineSelected());
        bar.add(payFine);
        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refresh());
        bar.add(refreshBtn);
        return bar;
    }

    public void refresh() {
        try {
            List<Loan> loans = loanDao.findActiveAndOverdue();
            tableModel.setRowCount(0);
            for (Loan l : loans) {
                tableModel.addRow(new Object[]{
                        l.getLoanId(), l.getBookTitle(), l.getMemberName(), l.getMembershipNo(),
                        l.getLoanDate(), l.getDueDate(), l.getReturnDate(), l.getStatus(),
                        l.getFineAmount(), l.isFinePaid()
                });
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void checkoutDialog() {
        try {
            List<Book> books = bookDao.findAvailable("");
            List<Member> members = memberDao.findAll("");
            if (books.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No books available to loan.");
                return;
            }
            if (members.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No members registered.");
                return;
            }
            JComboBox<Book> bookBox = new JComboBox<>(books.toArray(new Book[0]));
            JComboBox<Member> memberBox = new JComboBox<>(members.toArray(new Member[0]));
            memberBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                                boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof Member m) {
                        setText(m.toString() + " [" + m.getStatus() + "]");
                    }
                    return this;
                }
            });
            JTextField dueField = new JTextField(LocalDate.now().plusDays(14).toString(), 12);
            JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
            panel.add(new JLabel("Book:"));
            panel.add(bookBox);
            panel.add(new JLabel("Member:"));
            panel.add(memberBox);
            panel.add(new JLabel("Due date (YYYY-MM-DD):"));
            panel.add(dueField);
            if (JOptionPane.showConfirmDialog(this, panel, "Check Out", JOptionPane.OK_CANCEL_OPTION)
                    != JOptionPane.OK_OPTION) {
                return;
            }
            Book book = (Book) bookBox.getSelectedItem();
            Member member = (Member) memberBox.getSelectedItem();
            LocalDate due = LocalDate.parse(dueField.getText().trim());
            loanDao.checkout(book.getBookId(), member.getMemberId(), staff.getStaffId(), due);
            JOptionPane.showMessageDialog(this, "Book checked out successfully.");
            refresh();
        } catch (Exception ex) {
            showError(ex);
        }
    }

    private void returnSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a loan to return.");
            return;
        }
        int loanId = (int) tableModel.getValueAt(row, 0);
        try {
            loanDao.returnBook(loanId);
            JOptionPane.showMessageDialog(this, "Book returned. Overdue fines are recorded automatically.");
            refresh();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void payFineSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return;
        }
        int loanId = (int) tableModel.getValueAt(row, 0);
        try {
            loanDao.markFinePaid(loanId);
            JOptionPane.showMessageDialog(this, "Fine marked as paid.");
            refresh();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
