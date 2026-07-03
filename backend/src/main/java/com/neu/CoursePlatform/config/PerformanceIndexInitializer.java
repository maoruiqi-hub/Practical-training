package com.neu.CoursePlatform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PerformanceIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PerformanceIndexInitializer.class);
    private static final Pattern INDEX_PATTERN = Pattern.compile(
            "(?is)^CREATE\\s+(UNIQUE\\s+)?INDEX\\s+IF\\s+NOT\\s+EXISTS\\s+([\\w`\".]+)\\s+ON\\s+([\\w`\".]+)\\s*\\(");

    private final DataSource dataSource;
    private final ResourceLoader resourceLoader;
    private final boolean enabled;
    private final boolean failFast;
    private final String scriptLocation;

    public PerformanceIndexInitializer(DataSource dataSource,
                                       ResourceLoader resourceLoader,
                                       @Value("${app.performance-indexes.enabled:true}") boolean enabled,
                                       @Value("${app.performance-indexes.fail-fast:false}") boolean failFast,
                                       @Value("${app.performance-indexes.script:classpath:performance-indexes.sql}") String scriptLocation) {
        this.dataSource = dataSource;
        this.resourceLoader = resourceLoader;
        this.enabled = enabled;
        this.failFast = failFast;
        this.scriptLocation = scriptLocation;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Performance index initializer disabled.");
            return;
        }

        Resource resource = resourceLoader.getResource(scriptLocation);
        if (!resource.exists()) {
            log.warn("Performance index script not found: {}", scriptLocation);
            return;
        }

        List<String> statements;
        try {
            statements = splitStatements(resource.getContentAsString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            if (failFast) throw new IllegalStateException("Failed to read performance index script", e);
            log.warn("Failed to read performance index script: {}", scriptLocation, e);
            return;
        }

        int applied = 0;
        int skipped = 0;
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            String database = connection.getMetaData().getDatabaseProductName();
            for (String sql : statements) {
                String executableSql = adaptSql(connection, database, sql);
                if (executableSql == null) {
                    skipped++;
                    continue;
                }
                try {
                    statement.execute(executableSql);
                    applied++;
                } catch (SQLException e) {
                    skipped++;
                    if (failFast) {
                        throw new IllegalStateException("Failed to apply performance index: " + summarize(sql), e);
                    }
                    log.warn("Skipped performance index on {}: {} ({})", database, summarize(sql), e.getMessage());
                }
            }
            log.info("Performance indexes initialized. applied={}, skipped={}", applied, skipped);
        } catch (SQLException e) {
            if (failFast) throw new IllegalStateException("Failed to initialize performance indexes", e);
            log.warn("Failed to initialize performance indexes, application will continue.", e);
        }
    }

    private List<String> splitStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (String line : script.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--")) continue;
            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                if (ch == '\'' && !inDoubleQuote) {
                    inSingleQuote = !inSingleQuote;
                } else if (ch == '"' && !inSingleQuote) {
                    inDoubleQuote = !inDoubleQuote;
                }
                if (ch == ';' && !inSingleQuote && !inDoubleQuote) {
                    addStatement(statements, current);
                } else {
                    current.append(ch);
                }
            }
            current.append('\n');
        }
        addStatement(statements, current);
        return statements;
    }

    private void addStatement(List<String> statements, StringBuilder current) {
        String sql = current.toString().trim();
        if (!sql.isEmpty()) statements.add(sql);
        current.setLength(0);
    }

    private String summarize(String sql) {
        String normalized = sql.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 140 ? normalized : normalized.substring(0, 137) + "...";
    }

    private String adaptSql(Connection connection, String database, String sql) throws SQLException {
        String lowerDatabase = database == null ? "" : database.toLowerCase(Locale.ROOT);
        if (!lowerDatabase.contains("mysql")) return sql;
        if (sql.toLowerCase(Locale.ROOT).contains(" where ")) {
            log.warn("Skipped partial performance index on MySQL: {}", summarize(sql));
            return null;
        }
        Matcher matcher = INDEX_PATTERN.matcher(sql);
        if (!matcher.find()) return sql;

        String indexName = cleanIdentifier(matcher.group(2));
        String tableName = cleanIdentifier(matcher.group(3));
        if (indexExists(connection.getMetaData(), tableName, indexName)) {
            return null;
        }
        return sql.replaceFirst("(?i)INDEX\\s+IF\\s+NOT\\s+EXISTS", "INDEX");
    }

    private boolean indexExists(DatabaseMetaData metaData, String tableName, String indexName) throws SQLException {
        try (ResultSet indexes = metaData.getIndexInfo(null, null, tableName, false, false)) {
            while (indexes.next()) {
                String existing = indexes.getString("INDEX_NAME");
                if (existing != null && existing.equalsIgnoreCase(indexName)) return true;
            }
        }
        return false;
    }

    private String cleanIdentifier(String identifier) {
        if (identifier == null) return "";
        String cleaned = identifier.replace("`", "").replace("\"", "");
        int dot = cleaned.lastIndexOf('.');
        return dot >= 0 ? cleaned.substring(dot + 1) : cleaned;
    }
}
