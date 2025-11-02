package com.example.pocket.orm;

import com.example.pocket.orm.EntityMetadata;
import com.example.pocket.orm.FieldMetadata;

import java.util.*;

/**
 * SqlQueryParser is responsible for translating entity operations and queries into SQL statements.
 * It uses metadata loaded from JSON configuration to dynamically generate SQL.
 */
public class SqlQueryParser {

    private final Map<String, EntityMetadata> metadataMap;

    /**
     * Constructor to initialize the parser with metadata.
     *
     * @param metadataMap A map of entity names to their metadata.
     */
    public SqlQueryParser(Map<String, EntityMetadata> metadataMap) {
        this.metadataMap = metadataMap;
    }

    /**
     * Generates an INSERT SQL statement for the given entity.
     *
     * @param entityName The name of the entity.
     * @return The generated SQL INSERT statement.
     */
    public String generateInsertSql(String entityName) {
        EntityMetadata metadata = metadataMap.get(entityName);
        if (metadata == null) {
            throw new IllegalArgumentException("No metadata found for entity: " + entityName);
        }

        StringJoiner columns = new StringJoiner(", ");
        StringJoiner values = new StringJoiner(", ");

        metadata.getFields().forEach(field -> {
            columns.add(field.getColumnName());
            values.add("?"); // Placeholder for prepared statement
        });

        return String.format("INSERT INTO %s (%s) VALUES (%s)",
                metadata.getTableName(),
                columns.toString(),
                values.toString());
    }

    /**
     * Generates a SELECT SQL statement for the given entity and primary key.
     *
     * @param entityName The name of the entity.
     * @return The generated SQL SELECT statement.
     */
    public String generateSelectByIdSql(String entityName) {
        EntityMetadata metadata = metadataMap.get(entityName);
        if (metadata == null) {
            throw new IllegalArgumentException("No metadata found for entity: " + entityName);
        }

        return String.format("SELECT * FROM %s WHERE %s = ?",
                metadata.getTableName(),
                metadata.getPrimaryKeyColumn());
    }

    /**
     * Generates a DELETE SQL statement for the given entity and primary key.
     *
     * @param entityName The name of the entity.
     * @return The generated SQL DELETE statement.
     */
    public String generateDeleteByIdSql(String entityName) {
        EntityMetadata metadata = metadataMap.get(entityName);
        if (metadata == null) {
            throw new IllegalArgumentException("No metadata found for entity: " + entityName);
        }

        return String.format("DELETE FROM %s WHERE %s = ?",
                metadata.getTableName(),
                metadata.getPrimaryKeyColumn());
    }

    /**
     * Generates a SELECT SQL statement to fetch all rows for the given entity.
     *
     * @param entityName The name of the entity.
     * @return The generated SQL SELECT statement.
     */
    public String generateSelectAllSql(String entityName) {
        EntityMetadata metadata = metadataMap.get(entityName);
        if (metadata == null) {
            throw new IllegalArgumentException("No metadata found for entity: " + entityName);
        }

        return String.format("SELECT * FROM %s", metadata.getTableName());
    }

    /**
     * Parses an entity object into a Map representation for SQL operations.
     *
     * @param entity The entity object to parse.
     * @param entityName The name of the entity.
     * @return A Map where keys are column names and values are field values.
     */
    public Map<String, Object> parseEntityToMap(Object entity, String entityName) {
        EntityMetadata metadata = metadataMap.get(entityName);
        if (metadata == null) {
            throw new IllegalArgumentException("No metadata found for entity: " + entityName);
        }

        Map<String, Object> fieldMap = new HashMap<>();

        metadata.getFields().forEach(field -> {
            try {
                String fieldName = field.getFieldName();
                String columnName = field.getColumnName();
                Object value = entity.getClass().getDeclaredField(fieldName).get(entity);
                fieldMap.put(columnName, value);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Error accessing field: " + field.getFieldName(), e);
            }
        });

        return fieldMap;
    }

    /**
     * Parses a Map representation of an entity into a QueryContext, which contains all necessary information
     * for building SQL queries, including handling dependencies, ID generation, and relations.
     *
     * @param entityMap The Map representation of the entity, where keys are field names and values are field values.
     * @param entityName The name of the entity.
     * @return A QueryContext containing parsed information about the entity.
     */
    public QueryContext parseMapToQueryContext(Map<String, Object> entityMap, String entityName) {
        EntityMetadata metadata = metadataMap.get(entityName);
        if (metadata == null) {
            throw new IllegalArgumentException("No metadata found for entity: " + entityName);
        }

        QueryContext queryContext = new QueryContext(entityName, metadata.getTableName());

        metadata.getFields().forEach(field -> {
            String fieldName = field.getFieldName();
            String columnName = field.getColumnName();
            Object value = entityMap.get(fieldName);

            if (field.isPrimaryKey() && value == null) {
                // Handle ID generation for primary keys
                value = generateId();
            }

            queryContext.addField(columnName, value, field.isPrimaryKey(), field.isNullable(), field.isUnique());

            // Check if the field is a foreign key by verifying if it has foreign key metadata
            if (field.getForeignKeyReferenceTable() != null && field.getForeignKeyReferenceColumn() != null) {
                // Handle relations and dependencies
                queryContext.addDependency(field.getForeignKeyReferenceTable(), field.getForeignKeyReferenceColumn(), value);
            }
        });

        return queryContext;
    }

    /**
     * Generates a unique ID for primary key fields.
     * This is a placeholder implementation and can be replaced with a more robust strategy.
     *
     * @return A generated unique ID.
     */
    private Object generateId() {
        return UUID.randomUUID().toString();
    }

    /* ===================== New Map-based Persistence Parsing ===================== */

    /**
     * Holder for SQL string plus ordered parameter values ready for PreparedStatement binding.
     */
    public static class ParsedSql {
        private final String sql;
        private final List<Object> parameters;

        public ParsedSql(String sql, List<Object> parameters) {
            this.sql = sql;
            this.parameters = parameters;
        }
        public String getSql() { return sql; }
        public List<Object> getParameters() { return parameters; }
    }

    /**
     * Build an INSERT statement and params from a field map keyed by logical field names (not column names).
     * Will auto-generate primary key if missing.
     */
    public ParsedSql buildInsertFromMap(String entityName, Map<String, Object> input) {
        EntityMetadata meta = requireMeta(entityName);
        List<String> colNames = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (FieldMetadata f : meta.getFields()) {
            Object val = input.get(f.getFieldName());
            if (f.isPrimaryKey() && val == null) {
                val = generateId();
                input.put(f.getFieldName(), val); // reflect back for caller
            }
            if (val == null && !f.isNullable() && !f.isPrimaryKey()) {
                throw new IllegalArgumentException("Required field '" + f.getFieldName() + "' is null for entity " + entityName);
            }
            colNames.add(f.getColumnName());
            params.add(val);
        }
        String sql = "INSERT INTO " + meta.getTableName() + " (" + String.join(",", colNames) + ") VALUES (" + repeat("?", colNames.size()) + ")";
        return new ParsedSql(sql, params);
    }

    /**
     * Build an UPDATE statement based on provided map. Only includes non-PK fields; PK is used in WHERE.
     */
    public ParsedSql buildUpdateFromMap(String entityName, Map<String, Object> input) {
        EntityMetadata meta = requireMeta(entityName);
        FieldMetadata pkField = meta.getFields().stream().filter(FieldMetadata::isPrimaryKey).findFirst().orElse(null);
        if (pkField == null) throw new IllegalStateException("No primary key defined for entity " + entityName);
        Object pkValue = input.get(pkField.getFieldName());
        if (pkValue == null) throw new IllegalArgumentException("Primary key value missing for update of entity " + entityName);
        List<String> assignments = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (FieldMetadata f : meta.getFields()) {
            if (f.isPrimaryKey()) continue;
            if (!input.containsKey(f.getFieldName())) continue; // skip absent -> partial update
            assignments.add(f.getColumnName() + "=?");
            params.add(input.get(f.getFieldName()));
        }
        if (assignments.isEmpty()) throw new IllegalArgumentException("No fields provided to update for entity " + entityName);
        String sql = "UPDATE " + meta.getTableName() + " SET " + String.join(",", assignments) + " WHERE " + pkField.getColumnName() + "=?";
        params.add(pkValue); // PK last
        return new ParsedSql(sql, params);
    }

    /**
     * Build a DELETE statement by primary key.
     */
    public ParsedSql buildDeleteById(String entityName, Object id) {
        EntityMetadata meta = requireMeta(entityName);
        String sql = "DELETE FROM " + meta.getTableName() + " WHERE " + meta.getPrimaryKeyColumn() + "=?";
        return new ParsedSql(sql, Collections.singletonList(id));
    }

    /**
     * Build a SELECT statement with optional simple equals filters on provided map (logical field names).
     * Empty filter map selects all.
     */
    public ParsedSql buildSelectFromFilters(String entityName, Map<String,Object> filters) {
        EntityMetadata meta = requireMeta(entityName);
        if (filters == null || filters.isEmpty()) {
            return new ParsedSql("SELECT * FROM " + meta.getTableName(), Collections.emptyList());
        }
        List<String> predicates = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (FieldMetadata f : meta.getFields()) {
            if (filters.containsKey(f.getFieldName())) {
                predicates.add(f.getColumnName() + "=?");
                params.add(filters.get(f.getFieldName()));
            }
        }
        if (predicates.isEmpty()) {
            return new ParsedSql("SELECT * FROM " + meta.getTableName(), Collections.emptyList());
        }
        String sql = "SELECT * FROM " + meta.getTableName() + " WHERE " + String.join(" AND ", predicates);
        return new ParsedSql(sql, params);
    }

    private EntityMetadata requireMeta(String entityName) {
        EntityMetadata m = metadataMap.get(entityName);
        if (m == null) throw new IllegalArgumentException("Unknown entity '" + entityName + "'");
        return m;
    }

    private String repeat(String token, int count) {
        return String.join(",", Collections.nCopies(count, token));
    }
}
