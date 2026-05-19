package com.library.db;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class DatabaseInitializer {

    private DatabaseInitializer() {
    }

    /**
     * Creates tables and seed data on first run (when staff table is empty).
     * You must create the database {@code library_db} in MySQL before starting the app.
     */
    public static void initializeIfNeeded() throws SQLException, IOException {
        Path root = DatabaseConnection.getConfig().getProjectRoot();
        Path schemaFile = root.resolve("sql").resolve("schema.sql");
        Path seedFile = root.resolve("sql").resolve("seed.sql");

        if (!Files.exists(schemaFile)) {
            throw new IOException("Schema file not found: " + schemaFile);
        }

        try (Connection conn = DatabaseConnection.open()) {
            if (!isInitialized(conn)) {
                runScript(conn, Files.readString(schemaFile, StandardCharsets.UTF_8));
                if (Files.exists(seedFile)) {
                    runScript(conn, Files.readString(seedFile, StandardCharsets.UTF_8));
                }
            }
            refreshOverdueLoans(conn);
        }
    }

    private static boolean isInitialized(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) AS c FROM staff")) {
            return rs.next() && rs.getInt("c") > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    public static void refreshOverdueLoans(Connection conn) throws SQLException {
        String sql = """
                UPDATE loan SET status = 'OVERDUE'
                WHERE status = 'ACTIVE' AND return_date IS NULL AND due_date < CURDATE()
                """;
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql);
        }
    }

    private static void runScript(Connection conn, String script) throws SQLException {
        List<String> statements = splitStatements(script);
        try (Statement st = conn.createStatement()) {
            for (String sql : statements) {
                if (!sql.isBlank()) {
                    st.execute(sql);
                }
            }
        }
    }

    static List<String> splitStatements(String script) {
        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : script.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("--") || trimmed.isEmpty()) {
                continue;
            }
            current.append(line).append('\n');
            if (trimmed.endsWith(";")) {
                out.add(current.toString().trim());
                current.setLength(0);
            }
        }
        if (!current.isEmpty()) {
            out.add(current.toString().trim());
        }
        return out;
    }
}
