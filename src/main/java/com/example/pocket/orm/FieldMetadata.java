package com.example.pocket.orm;

/**
 * Represents metadata for a field in an entity, including column name, type, and constraints.
 */
public class FieldMetadata {

    private String fieldName;
    private String columnName;
    private String columnType;
    private boolean isPrimaryKey;
    private boolean isNullable;
    private boolean isUnique;
    // Optional FK reference (table + column) if this field is a foreign key
    private String foreignKeyReferenceTable;
    private String foreignKeyReferenceColumn;

    public FieldMetadata(String fieldName, String columnName, String columnType, boolean isPrimaryKey, boolean isNullable, boolean isUnique) {
        this(fieldName, columnName, columnType, isPrimaryKey, isNullable, isUnique, null, null);
    }

    public FieldMetadata(String fieldName, String columnName, String columnType, boolean isPrimaryKey, boolean isNullable, boolean isUnique, String foreignKeyReferenceTable, String foreignKeyReferenceColumn) {
        this.fieldName = fieldName;
        this.columnName = columnName;
        this.columnType = columnType;
        this.isPrimaryKey = isPrimaryKey;
        this.isNullable = isNullable;
        this.isUnique = isUnique;
        this.foreignKeyReferenceTable = foreignKeyReferenceTable;
        this.foreignKeyReferenceColumn = foreignKeyReferenceColumn;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        isPrimaryKey = primaryKey;
    }

    public boolean isNullable() {
        return isNullable;
    }

    public void setNullable(boolean nullable) {
        isNullable = nullable;
    }

    public boolean isUnique() {
        return isUnique;
    }

    public void setUnique(boolean unique) {
        isUnique = unique;
    }

    public String getForeignKeyReferenceTable() {
        return foreignKeyReferenceTable;
    }

    public void setForeignKeyReferenceTable(String foreignKeyReferenceTable) {
        this.foreignKeyReferenceTable = foreignKeyReferenceTable;
    }

    public String getForeignKeyReferenceColumn() {
        return foreignKeyReferenceColumn;
    }

    public void setForeignKeyReferenceColumn(String foreignKeyReferenceColumn) {
        this.foreignKeyReferenceColumn = foreignKeyReferenceColumn;
    }

    public static FieldMetadata pk(String fieldName, String columnName, String columnType) {
        return new FieldMetadata(fieldName, columnName, columnType, true, false, true);
    }

    public static FieldMetadata fk(String fieldName, String columnName, String columnType, String refTable, String refColumn) {
        return new FieldMetadata(fieldName, columnName, columnType, false, false, false, refTable, refColumn);
    }

    public static FieldMetadata regular(String fieldName, String columnName, String columnType, boolean nullable, boolean unique) {
        return new FieldMetadata(fieldName, columnName, columnType, false, nullable, unique);
    }
}
