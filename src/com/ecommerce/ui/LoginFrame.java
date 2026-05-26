package com.ecommerce.ui;

import com.ecommerce.dao.UserDao;
import com.ecommerce.model.User;

import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.function.Consumer;

public class LoginFrame extends JFrame {

    private final JTextField usernameField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final Consumer<User> onSuccess;

    public LoginFrame(Consumer<User> onSuccess) {
        this.onSuccess = onSuccess;
        setTitle("Mini E-Commerce - Login");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        buildUi();
        pack();
        setLocationRelativeTo(null);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(24, 32, 24, 32));

        JLabel title = new JLabel("Mini E-Commerce System");
        title.setFont(UiStyles.TITLE_FONT);
        title.setForeground(UiStyles.PRIMARY);
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 4, 6, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        form.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        form.add(passwordField, gbc);

        root.add(form, BorderLayout.CENTER);

        JLabel hint = new JLabel("<html>Demo: <b>admin</b>/<b>admin</b> or <b>customer</b>/<b>customer</b></html>");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(Color.GRAY);

        JPanel south = new JPanel(new BorderLayout(8, 0));
        south.add(hint, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        JButton loginBtn = new JButton("Login");
        loginBtn.setBackground(UiStyles.PRIMARY);
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);
        loginBtn.addActionListener(e -> attemptLogin());
        passwordField.addActionListener(e -> attemptLogin());
        buttons.add(loginBtn);
        south.add(buttons, BorderLayout.EAST);
        root.add(south, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void attemptLogin() {
        String user = usernameField.getText();
        String pass = new String(passwordField.getPassword());
        if (user.isBlank() || pass.isBlank()) {
            JOptionPane.showMessageDialog(this, "Enter username and password.", "Login", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            UserDao dao = new UserDao();
            var u = dao.authenticate(user, pass);
            if (u.isPresent()) {
                dispose();
                onSuccess.accept(u.get());
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials.", "Login", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Login", JOptionPane.ERROR_MESSAGE);
        }
    }
}

