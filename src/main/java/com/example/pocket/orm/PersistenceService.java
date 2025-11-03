package com.example.pocket.orm;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * High-level persistence service: builds insert plans from parser and executes them transactionally.
 */
public class PersistenceService {

    private final SqlQueryParser parser;
    private final SqlExecutor executor;

    public PersistenceService(SqlQueryParser parser, Connection conn) {
        this.parser = parser;
        this.executor = new SqlExecutor(conn);
    }

    public ExecutionResult persist(String entityName, Map<String,Object> input) throws SQLException {
        List<ParsedSql> plan = parser.buildInsertPlan(entityName, input);
        return executor.executeInTransaction(plan, true);
    }
}
