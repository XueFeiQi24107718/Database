package com.ecommerce.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class DatabaseConfig {

    private final Properties properties = new Properties();
    private final Path projectRoot;

    public DatabaseConfig(Path projectRoot) {
        this.projectRoot = projectRoot;
        load();
    }

    private void load() {
        Path configFile = projectRoot.resolve("config").resolve("db.properties");
        if (Files.exists(configFile)) {
            try (InputStream in = Files.newInputStream(configFile)) {
                properties.load(in);
                return;
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read config/db.properties", e);
            }
        }
        throw new IllegalStateException(
                "Missing config/db.properties. Copy config/db.properties and set your MySQL password.");
    }

    public String getUrl() {
        return properties.getProperty("db.url");
    }

    public String getUser() {
        return properties.getProperty("db.user", "root");
    }

    public String getPassword() {
        return properties.getProperty("db.password", "");
    }

    public Path getProjectRoot() {
        return projectRoot;
    }

    public void validate() {
        if (getUrl() == null || getUrl().isBlank()) {
            throw new IllegalStateException("db.url is not set in config/db.properties");
        }
        if ("your_password_here".equals(getPassword())) {
            throw new IllegalStateException(
                    "Set your real MySQL password in config/db.properties (db.password=...)");
        }
    }
}

