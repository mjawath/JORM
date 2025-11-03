package com.example.pocket.orm;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes parameterized SQL statements against a JDBC connection.
 */
public class SqlExecutor {

    private final Connection conn;

    public SqlExecutor(Connection conn) {
        this.conn = conn;
    }

    /**
     * Execute a single parsed SQL statement. Optionally request generated keys retrieval.
     */
    public ExecutionResult execute(ParsedSql parsed, boolean returnGeneratedKeys) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(parsed.getSql(), returnGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS)) {
            bindParameters(ps, parsed.getParameters());
            int affected = ps.executeUpdate();
            List<Object> keys = new ArrayList<>();
            if (returnGeneratedKeys) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    while (rs.next()) {
                        keys.add(rs.getObject(1));
                    }
                }
            }
            return new ExecutionResult(affected, keys);
        }
    }

    /**
     * Execute a list of parsed SQL statements inside a transaction. If any statement fails, rollback.
     * Return the result for the last statement and aggregate generated keys from all statements when requested.
     */
    public ExecutionResult executeInTransaction(List<ParsedSql> statements, boolean returnGeneratedKeys) throws SQLException {
        boolean prevAuto = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);
            int totalAffected = 0;
            List<Object> allKeys = new ArrayList<>();
            for (int i = 0; i < statements.size(); i++) {
                ParsedSql p = statements.get(i);
                try (PreparedStatement ps = conn.prepareStatement(p.getSql(), returnGeneratedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS)) {
                    bindParameters(ps, p.getParameters());
                    int affected = ps.executeUpdate();
                    totalAffected += affected;
                    if (returnGeneratedKeys) {
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            while (rs.next()) {
                                allKeys.add(rs.getObject(1));
                            }
                        }
                    }
                }
            }
            conn.commit();
            return new ExecutionResult(totalAffected, allKeys);
        } catch (SQLException ex) {
            conn.rollback();
            throw ex;
        } finally {
            conn.setAutoCommit(prevAuto);
        }
    }

    private void bindParameters(PreparedStatement ps, List<Object> params) throws SQLException {
        if (params == null) return;
        for (int i = 0; i < params.size(); i++) {
            Object v = params.get(i);
            ps.setObject(i + 1, v);
        }
    }
}
