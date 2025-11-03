package com.example.pocket.orm;

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

    // ParsedSql moved to top-level class `ParsedSql` to be shared across executor and builder.

    /**
     * Build an INSERT statement and params from a field map keyed by logical field names (not column names).
     * Will auto-generate primary key if missing.
     */
    public ParsedSql buildInsertFromMap(String entityName, Map<String, Object> input) {
        EntityMetadata meta = requireMeta(entityName);
        // ensure PK generation/validation as before
        for (FieldMetadata f : meta.getFields()) {
            if (f.isPrimaryKey() && (!input.containsKey(f.getFieldName()) || input.get(f.getFieldName()) == null)) {
                input.put(f.getFieldName(), generateId());
            }
            if (!f.isPrimaryKey() && !f.isNullable() && !input.containsKey(f.getFieldName())) {
                throw new IllegalArgumentException("Required field '" + f.getFieldName() + "' is missing for entity " + entityName);
            }
        }
        SqlBuilder builder = new SqlBuilder();
        SqlStatement stmt = builder.buildInsert(meta, meta.getFields(), input);
        return new ParsedSql(stmt.getSql(), stmt.getParameters());
    }

    /**
     * Build an UPDATE statement based on provided map. Only includes non-PK fields; PK is used in WHERE.
     */
    public ParsedSql buildUpdateFromMap(String entityName, Map<String, Object> input) {
        EntityMetadata meta = requireMeta(entityName);
        SqlBuilder builder = new SqlBuilder();
        SqlStatement stmt = builder.buildUpdate(meta, meta.getFields(), input);
        return new ParsedSql(stmt.getSql(), stmt.getParameters());
    }

    /**
     * Build a DELETE statement by primary key.
     */
    public ParsedSql buildDeleteById(String entityName, Object id) {
        EntityMetadata meta = requireMeta(entityName);
        SqlBuilder builder = new SqlBuilder();
        SqlStatement stmt = builder.buildDeleteById(meta, id);
        return new ParsedSql(stmt.getSql(), stmt.getParameters());
    }

    /**
     * Build a SELECT statement with optional simple equals filters on provided map (logical field names).
     * Empty filter map selects all.
     */
    public ParsedSql buildSelectFromFilters(String entityName, Map<String,Object> filters) {
        EntityMetadata meta = requireMeta(entityName);
        // simple implementation: build WHERE on equals
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

    /**
     * Builds an ordered insert plan (list of ParsedSql) for nested input maps.
     * Master/parent entities are emitted before their children so inserted PKs can be propagated.
     *
     * Input map may contain nested Map or List<Map> values for child relations. Child fields must be declared
     * in metadata with foreign key references pointing to the parent's table/column.
     *
     * Returns a list of ParsedSql objects in the order they should be executed.
     */
    public List<ParsedSql> buildInsertPlan(String entityName, Map<String,Object> input) {
        List<ParsedSql> plan = new ArrayList<>();
        Map<String,Object> generatedIds = new HashMap<>();
        buildInsertPlanRec(entityName, input, plan, generatedIds, null);
        return plan;
    }

    private void buildInsertPlanRec(String entityName, Map<String,Object> input, List<ParsedSql> plan, Map<String,Object> generatedIds, String parentRefColumn) {
        EntityMetadata meta = requireMeta(entityName);
        // Ensure PK exists
        FieldMetadata pkField = meta.getFields().stream().filter(FieldMetadata::isPrimaryKey).findFirst().orElse(null);
        if (pkField == null) throw new IllegalStateException("Entity " + entityName + " has no primary key");
        if (!input.containsKey(pkField.getFieldName()) || input.get(pkField.getFieldName()) == null) {
            Object newId = generateId();
            input.put(pkField.getFieldName(), newId);
            generatedIds.put(entityName + ":" + pkField.getFieldName(), newId);
        }

        // If parentRefColumn is provided, set the FK field(s) that point to that parent
        if (parentRefColumn != null) {
            for (FieldMetadata f : meta.getFields()) {
                if (parentRefColumn.equals(f.getForeignKeyReferenceColumn()) || parentRefColumn.equals(f.getForeignKeyReferenceTable())) {
                    // if child expects parent id under a different logical field name, try to set it
                    if (!input.containsKey(f.getFieldName())) {
                        // try parent id lookup
                        Object parentId = generatedIds.get(entityName + ":" + f.getFieldName());
                        if (parentId != null) input.put(f.getFieldName(), parentId);
                    }
                }
            }
        }

        // Build this entity's insert SQL and append to plan
        ParsedSql thisInsert = buildInsertFromMap(entityName, input);
        plan.add(thisInsert);

        // Now discover nested children: any field in input which is Map or List<Map>
        for (Map.Entry<String,Object> e : new ArrayList<>(input.entrySet())) {
            Object v = e.getValue();
            if (v instanceof Map) {
                // Child entity name must be inferable: assume the fieldName equals child entity name
                @SuppressWarnings("unchecked")
                Map<String,Object> childMap = (Map<String,Object>) v;
                String childEntityName = e.getKey();
                // Propagate parent's PK into child's FK fields when their FK metadata points to this meta
                EntityMetadata childMeta = metadataMap.get(childEntityName);
                if (childMeta != null) {
                    FieldMetadata childFk = childMeta.getFields().stream().filter(fm -> {
                        return meta.getTableName().equals(fm.getForeignKeyReferenceTable()) || meta.getPrimaryKeyColumn().equals(fm.getForeignKeyReferenceColumn());
                    }).findFirst().orElse(null);
                    if (childFk != null && !childMap.containsKey(childFk.getFieldName())) {
                        childMap.put(childFk.getFieldName(), input.get(pkField.getFieldName()));
                    }
                }
                buildInsertPlanRec(childEntityName, childMap, plan, generatedIds, pkField.getFieldName());
            } else if (v instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) v;
                for (Object item : list) {
                    if (item instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String,Object> childMap = (Map<String,Object>) item;
                        String childEntityName = e.getKey();
                        EntityMetadata childMeta = metadataMap.get(childEntityName);
                        if (childMeta != null) {
                            FieldMetadata childFk = childMeta.getFields().stream().filter(fm -> {
                                return meta.getTableName().equals(fm.getForeignKeyReferenceTable()) || meta.getPrimaryKeyColumn().equals(fm.getForeignKeyReferenceColumn());
                            }).findFirst().orElse(null);
                            if (childFk != null && !childMap.containsKey(childFk.getFieldName())) {
                                childMap.put(childFk.getFieldName(), input.get(pkField.getFieldName()));
                            }
                        }
                        buildInsertPlanRec(childEntityName, childMap, plan, generatedIds, pkField.getFieldName());
                    }
                }
            }
        }
    }

}
