package com.example.pocket.orm;

import java.sql.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

/**
 * Provides methods to execute SQL queries synchronously and asynchronously using JDBC and ExecutorsProvider.
 */
public class QueryExecutor {
    private final Connection connection;

    public QueryExecutor(Connection connection) {
        this.connection = connection;
    }

    // Synchronous SELECT query
    public List<Object[]> executeSelect(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<Object[]> results = new ArrayList<>();
            int columnCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Object[] row = new Object[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    row[i - 1] = rs.getObject(i);
                }
                results.add(row);
            }
            return results;
        }
    }

    // Synchronous UPDATE/INSERT/DELETE query
    public int executeUpdate(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }

    // Asynchronous SELECT query
    public Future<List<Object[]>> executeSelectAsync(String sql) {
        return ExecutorsProvider.CACHED_THREAD_POOL.submit(new Callable<List<Object[]>>() {
            @Override
            public List<Object[]> call() throws Exception {
                return executeSelect(sql);
            }
        });
    }

    // Asynchronous UPDATE/INSERT/DELETE query
    public Future<Integer> executeUpdateAsync(String sql) {
        return ExecutorsProvider.CACHED_THREAD_POOL.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return executeUpdate(sql);
            }
        });
    }
}

