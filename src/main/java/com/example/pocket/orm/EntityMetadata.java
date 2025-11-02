package com.example.pocket.orm;

import java.util.List;

/**
 * Represents metadata for an entity, including table name, fields, and primary key information.
 */
public class EntityMetadata {

    private String entityName;
    private String tableName;
    private String primaryKeyColumn;
    private List<FieldMetadata> fields;

    public EntityMetadata(String entityName, String tableName, String primaryKeyColumn, List<FieldMetadata> fields) {
        this.entityName = entityName;
        this.tableName = tableName;
        this.primaryKeyColumn = primaryKeyColumn;
        this.fields = fields;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getPrimaryKeyColumn() {
        return primaryKeyColumn;
    }

    public void setPrimaryKeyColumn(String primaryKeyColumn) {
        this.primaryKeyColumn = primaryKeyColumn;
    }

    public List<FieldMetadata> getFields() {
        return fields;
    }

    public void setFields(List<FieldMetadata> fields) {
        this.fields = fields;
    }
}
