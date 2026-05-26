package com.ecommerce.app;

import com.ecommerce.db.DatabaseConnection;
import com.ecommerce.db.DatabaseInitializer;
import com.ecommerce.ui.LoginFrame;
import com.ecommerce.ui.MainFrame;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class  EcommerceApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            Path projectRoot = resolveProjectRoot();
            try {
                DatabaseConnection.configure(projectRoot);
                DatabaseInitializer.initializeIfNeeded();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null,
                        "Failed to connect to MySQL:\n" + ex.getMessage()
                                + "\n\n1. Install MySQL Server\n"
                                + "2. Run sql/00_create_database.sql\n"
                                + "3. Copy config/db.properties -> config/db.properties\n"
                                + "4. Edit config/db.properties (password)\n"
                                + "5. Put mysql-connector-j-*.jar in lib/\n\nSee README.md",
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
            new LoginFrame(u -> new MainFrame(u).setVisible(true)).setVisible(true);
        });
    }

    static Path resolveProjectRoot() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        if (hasSchema(cwd)) {
            return cwd;
        }
        if (cwd.getParent() != null && hasSchema(cwd.getParent())) {
            return cwd.getParent();
        }
        try {
            Path fromClass = Paths.get(EcommerceApp.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI()).normalize();
            if (fromClass.getParent() != null && fromClass.getParent().getParent() != null) {
                Path candidate = fromClass.getParent().getParent();
                if (hasSchema(candidate)) {
                    return candidate;
                }
            }
        } catch (Exception ignored) {
        }
        return cwd;
    }

    private static boolean hasSchema(Path root) {
        return root.resolve("sql").resolve("schema.sql").toFile().exists();
    }
}

