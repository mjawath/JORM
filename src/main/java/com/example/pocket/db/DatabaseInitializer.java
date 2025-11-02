package com.example.pocket.db;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer {
    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("PRAGMA foreign_keys=ON");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS collections (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT UNIQUE NOT NULL," +
                "schema_json TEXT," +
                "created_at TEXT NOT NULL," +
                "updated_at TEXT NOT NULL" +
                ")");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS records (" +
                "id TEXT PRIMARY KEY," +
                "collection_id INTEGER NOT NULL REFERENCES collections(id) ON DELETE CASCADE," +
                "data_json TEXT NOT NULL," +
                "created_at TEXT NOT NULL," +
                "updated_at TEXT NOT NULL" +
                ")");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_records_collection ON records(collection_id)");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS endpoints (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "method TEXT NOT NULL," +
                "path TEXT NOT NULL," +
                "collection_id INTEGER NOT NULL REFERENCES collections(id) ON DELETE CASCADE," +
                "created_at TEXT NOT NULL," +
                "updated_at TEXT NOT NULL" +
                ")");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_endpoints_method_path ON endpoints(method, path)");

        log.info("Database initialized");
    }
}
