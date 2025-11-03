package com.example.pocket.orm;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SqlExecutorTest {
    private Connection conn;

    @BeforeEach
    void setup() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement s = conn.createStatement()) {
            s.execute("CREATE TABLE widgets(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        conn.close();
    }

    @Test
    void executeSingleInsertWithGeneratedKey() throws Exception {
        SqlExecutor ex = new SqlExecutor(conn);
        ParsedSql ps = new ParsedSql("INSERT INTO widgets(name) VALUES(?)", List.of("w1"));
        ExecutionResult r = ex.execute(ps, true);
        assertEquals(1, r.getAffectedRows());
        assertFalse(r.getGeneratedKeys().isEmpty());
        assertNotNull(r.getGeneratedKeys().get(0));
    }

    @Test
    void executeInTransactionRollbackOnError() throws Exception {
        SqlExecutor ex = new SqlExecutor(conn);
        ParsedSql p1 = new ParsedSql("INSERT INTO widgets(name) VALUES(?)", List.of("a"));
        // second statement will violate NOT NULL if name is null or cause syntax error; using bad SQL to force failure
        ParsedSql p2 = new ParsedSql("INSER INTO widgets(name) VALUES(?)", List.of("b"));
        List<ParsedSql> plan = List.of(p1, p2);
        try {
            ex.executeInTransaction(plan, false);
            fail("Expected SQLException");
        } catch (Exception e) {
            // expected
        }
        // ensure no rows were inserted
        try (var rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM widgets")) {
            // JDBC SQLite returns a ResultSet; use query
            var stmt = conn.createStatement();
            var rs2 = stmt.executeQuery("SELECT COUNT(*) FROM widgets");
            rs2.next();
            int count = rs2.getInt(1);
            assertEquals(0, count);
            rs2.close();
            stmt.close();
        }
    }
}
