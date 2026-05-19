package com.library.ui;

import com.library.dao.LoanDao;
import com.library.model.Staff;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;

public class MainFrame extends JFrame {

    private final BookPanel bookPanel = new BookPanel();
    private final MemberPanel memberPanel = new MemberPanel();
    private final LoanPanel loanPanel;

    public MainFrame(Staff staff) {
        this.loanPanel = new LoanPanel(staff);
        setTitle("Library Management System - " + staff.getFullName());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1000, 620);
        setLocationRelativeTo(null);
        buildUi(staff);
    }

    private void buildUi(Staff staff) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UiStyles.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel title = new JLabel("Library Management System");
        title.setForeground(Color.WHITE);
        title.setFont(UiStyles.TITLE_FONT);
        header.add(title, BorderLayout.WEST);
        JLabel user = new JLabel("Logged in: " + staff.getFullName() + "  |  Role: " + staff.getRole());
        user.setForeground(Color.WHITE);
        header.add(user, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Books", bookPanel);
        tabs.addTab("Members", memberPanel);
        tabs.addTab("Loans", loanPanel);
        tabs.addTab("Dashboard", buildDashboard());

        tabs.addChangeListener(e -> {
            if (tabs.getSelectedComponent() == bookPanel) {
                bookPanel.refresh();
            } else if (tabs.getSelectedComponent() == memberPanel) {
                memberPanel.refresh();
            } else if (tabs.getSelectedComponent() == loanPanel) {
                loanPanel.refresh();
            }
        });

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(tabs, BorderLayout.CENTER);
    }

    private JPanel buildDashboard() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JLabel info = new JLabel("<html><h2>Dashboard</h2><p>Use the tabs to manage books, members, and loans.</p></html>");
        panel.add(info);
        try {
            int overdue = new LoanDao().countOverdue();
            JLabel stats = new JLabel("Overdue loans: " + overdue);
            stats.setFont(new Font("Segoe UI", Font.BOLD, 14));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.insets = new Insets(16, 0, 0, 0);
            panel.add(stats, gbc);
        } catch (SQLException ex) {
            panel.add(new JLabel("Could not load statistics: " + ex.getMessage()));
        }
        return panel;
    }
}
