package com.library.ui;

import com.library.dao.MemberDao;
import com.library.model.Member;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class MemberPanel extends JPanel {

    private final MemberDao memberDao = new MemberDao();
    private final JTextField searchField = new JTextField(20);
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"ID", "Membership No", "First Name", "Last Name", "Email", "Phone", "Joined", "Status"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(tableModel);

    public MemberPanel() {
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
        JButton addBtn = new JButton("Add Member");
        addBtn.addActionListener(e -> showDialog(null));
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
            List<Member> members = memberDao.findAll(searchField.getText());
            tableModel.setRowCount(0);
            for (Member m : members) {
                tableModel.addRow(new Object[]{
                        m.getMemberId(), m.getMembershipNo(), m.getFirstName(), m.getLastName(),
                        m.getEmail(), m.getPhone(), m.getJoinedDate(), m.getStatus()
                });
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a member first.");
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        try {
            memberDao.findById(id).ifPresentOrElse(this::showDialog, () ->
                    JOptionPane.showMessageDialog(this, "Member not found."));
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
        if (JOptionPane.showConfirmDialog(this, "Delete member?", "Confirm", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION) {
            try {
                memberDao.delete(id);
                refresh();
            } catch (SQLException ex) {
                showError(ex);
            }
        }
    }

    private void showDialog(Member existing) {
        JTextField memNo = new JTextField(existing != null ? existing.getMembershipNo() : "", 12);
        JTextField first = new JTextField(existing != null ? existing.getFirstName() : "", 12);
        JTextField last = new JTextField(existing != null ? existing.getLastName() : "", 12);
        JTextField email = new JTextField(existing != null ? existing.getEmail() : "", 16);
        JTextField phone = new JTextField(existing != null ? existing.getPhone() : "", 12);
        JComboBox<String> status = new JComboBox<>(new String[]{"ACTIVE", "SUSPENDED", "EXPIRED"});
        if (existing != null) {
            status.setSelectedItem(existing.getStatus());
        }
        JPanel panel = new JPanel(new GridLayout(0, 2, 6, 6));
        panel.add(new JLabel("Membership no:"));
        panel.add(memNo);
        panel.add(new JLabel("First name:"));
        panel.add(first);
        panel.add(new JLabel("Last name:"));
        panel.add(last);
        panel.add(new JLabel("Email:"));
        panel.add(email);
        panel.add(new JLabel("Phone:"));
        panel.add(phone);
        panel.add(new JLabel("Status:"));
        panel.add(status);
        if (JOptionPane.showConfirmDialog(this, panel, existing == null ? "Add Member" : "Edit Member",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            Member m = existing != null ? existing : new Member();
            m.setMembershipNo(memNo.getText().trim());
            m.setFirstName(first.getText().trim());
            m.setLastName(last.getText().trim());
            m.setEmail(email.getText().trim().isEmpty() ? null : email.getText().trim());
            m.setPhone(phone.getText().trim().isEmpty() ? null : phone.getText().trim());
            m.setStatus((String) status.getSelectedItem());
            if (existing == null) {
                m.setJoinedDate(LocalDate.now());
                memberDao.insert(m);
            } else {
                memberDao.update(m);
            }
            refresh();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    public Member getSelectedMember() throws SQLException {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        return memberDao.findById(id).orElse(null);
    }

    private void showError(SQLException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
