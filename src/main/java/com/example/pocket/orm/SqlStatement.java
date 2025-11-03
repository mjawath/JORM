package com.example.pocket.orm;

import java.util.Collections;
import java.util.List;

/**
 * Lightweight holder for a parameterized SQL statement and its ordered parameters.
 */
public class SqlStatement {
    private final String sql;
    private final List<Object> parameters;

    public SqlStatement(String sql, List<Object> parameters) {
        this.sql = sql;
        this.parameters = parameters == null ? Collections.emptyList() : parameters;
    }

    public String getSql() {
        return sql;
    }

    public List<Object> getParameters() {
        return parameters;
    }
}
