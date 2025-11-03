package com.example.pocket.orm;

import java.util.List;

public class ExecutionResult {
    private final int affectedRows;
    private final List<Object> generatedKeys;

    public ExecutionResult(int affectedRows, List<Object> generatedKeys) {
        this.affectedRows = affectedRows;
        this.generatedKeys = generatedKeys;
    }

    public int getAffectedRows() {
        return affectedRows;
    }

    public List<Object> getGeneratedKeys() {
        return generatedKeys;
    }
}
