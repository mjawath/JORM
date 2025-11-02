package com.example.pocket.orm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * QueryContext holds all the necessary information about an entity for building SQL queries.
 * It includes fields, dependencies, and metadata for handling relations and ID generation.
 */
public class QueryContext {

    private final String entityName;
    private final String tableName;
    private final Map<String, FieldContext> fields;
    private final List<DependencyContext> dependencies;

    public QueryContext(String entityName, String tableName) {
        this.entityName = entityName;
        this.tableName = tableName;
        this.fields = new HashMap<>();
        this.dependencies = new ArrayList<>();
    }

    public String getEntityName() {
        return entityName;
    }

    public String getTableName() {
        return tableName;
    }

    public Map<String, FieldContext> getFields() {
        return fields;
    }

    public List<DependencyContext> getDependencies() {
        return dependencies;
    }

    public void addField(String columnName, Object value, boolean isPrimaryKey, boolean isNullable, boolean isUnique) {
        fields.put(columnName, new FieldContext(columnName, value, isPrimaryKey, isNullable, isUnique));
    }

    public void addDependency(String referenceTable, String referenceColumn, Object value) {
        dependencies.add(new DependencyContext(referenceTable, referenceColumn, value));
    }

    /**
     * Represents a field in the entity, including its value and constraints.
     */
    public static class FieldContext {
        private final String columnName;
        private final Object value;
        private final boolean isPrimaryKey;
        private final boolean isNullable;
        private final boolean isUnique;

        public FieldContext(String columnName, Object value, boolean isPrimaryKey, boolean isNullable, boolean isUnique) {
            this.columnName = columnName;
            this.value = value;
            this.isPrimaryKey = isPrimaryKey;
            this.isNullable = isNullable;
            this.isUnique = isUnique;
        }

        public String getColumnName() {
            return columnName;
        }

        public Object getValue() {
            return value;
        }

        public boolean isPrimaryKey() {
            return isPrimaryKey;
        }

        public boolean isNullable() {
            return isNullable;
        }

        public boolean isUnique() {
            return isUnique;
        }
    }

    /**
     * Represents a dependency for handling relations, such as foreign keys.
     */
    public static class DependencyContext {
        private final String referenceTable;
        private final String referenceColumn;
        private final Object value;

        public DependencyContext(String referenceTable, String referenceColumn, Object value) {
            this.referenceTable = referenceTable;
            this.referenceColumn = referenceColumn;
            this.value = value;
        }

        public String getReferenceTable() {
            return referenceTable;
        }

        public String getReferenceColumn() {
            return referenceColumn;
        }

        public Object getValue() {
            return value;
        }
    }
}
