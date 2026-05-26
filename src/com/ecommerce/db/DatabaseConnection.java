package com.ecommerce.db;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private static DatabaseConfig config;

    private DatabaseConnection() {
    }

    public static void configure(Path projectRoot) {
        config = new DatabaseConfig(projectRoot);
        config.validate();
    }

    public static DatabaseConfig getConfig() {
        if (config == null) {
            throw new IllegalStateException("Database not configured. Call configure() first.");
        }
        return config;
    }

    public static Connection open() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "MySQL driver not found. Download mysql-connector-j-*.jar and place it in lib/. See README.md",
                    e);
        }
        return DriverManager.getConnection(
                getConfig().getUrl(),
                getConfig().getUser(),
                getConfig().getPassword());
    }
}

