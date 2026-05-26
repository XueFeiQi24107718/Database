package com.ecommerce.ui;

import com.ecommerce.model.User;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private final User user;
    private final ProductPanel productPanel;
    private final CartPanel cartPanel;
    private final OrdersPanel ordersPanel;
    private final CategoryPanel categoryPanel;

    public MainFrame(User user) {
        this.user = user;
        this.productPanel = new ProductPanel(user);
        this.cartPanel = user.isAdmin() ? null : new CartPanel(user);
        this.ordersPanel = new OrdersPanel(user);
        this.categoryPanel = user.isAdmin() ? new CategoryPanel() : null;

        setTitle("Mini E-Commerce - " + user.getUsername());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1050, 650);
        setLocationRelativeTo(null);
        buildUi();
    }

    private void buildUi() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UiStyles.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        JLabel title = new JLabel("Mini E-Commerce System");
        title.setForeground(Color.WHITE);
        title.setFont(UiStyles.TITLE_FONT);
        header.add(title, BorderLayout.WEST);

        JLabel who = new JLabel("Logged in: " + user.getUsername() + "  |  Role: " + user.getRole());
        who.setForeground(Color.WHITE);
        header.add(who, BorderLayout.EAST);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Products", productPanel);
        if (user.isAdmin()) {
            tabs.addTab("Categories", categoryPanel);
            tabs.addTab("Orders", ordersPanel);
        } else {
            tabs.addTab("Cart", cartPanel);
            tabs.addTab("My Orders", ordersPanel);
        }

        tabs.addChangeListener(e -> {
            Component c = tabs.getSelectedComponent();
            if (c == productPanel) {
                productPanel.refresh();
            } else if (c == cartPanel) {
                cartPanel.refresh();
            } else if (c == ordersPanel) {
                ordersPanel.refresh();
            } else if (c == categoryPanel) {
                categoryPanel.refresh();
                productPanel.refreshCategories();
            }
        });

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(header, BorderLayout.NORTH);
        getContentPane().add(tabs, BorderLayout.CENTER);
    }
}

