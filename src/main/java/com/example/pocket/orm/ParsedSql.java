package com.example.pocket.orm;

import java.util.List;

public class ParsedSql {
    private final String sql;
    private final List<Object> parameters;

    public ParsedSql(String sql, List<Object> parameters) {
        this.sql = sql;
        this.parameters = parameters;
    }

    public String getSql() {
        return sql;
    }

    public List<Object> getParameters() {
        return parameters;
    }
}
