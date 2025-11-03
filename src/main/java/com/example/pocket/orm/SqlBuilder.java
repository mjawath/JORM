package com.example.pocket.orm;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds parameterized SQL statements from entity metadata and input data.
 */
public class SqlBuilder {

    public SqlStatement buildInsert(EntityMetadata meta, List<FieldMetadata> fields, java.util.Map<String,Object> input) {
        List<String> cols = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (FieldMetadata f : fields) {
            cols.add(f.getColumnName());
            params.add(input.get(f.getFieldName()));
        }
        String sql = "INSERT INTO " + meta.getTableName() + " (" + String.join(",", cols) + ") VALUES (" + cols.stream().map(c->"?").collect(Collectors.joining(",")) + ")";
        return new SqlStatement(sql, params);
    }

    public SqlStatement buildUpdate(EntityMetadata meta, List<FieldMetadata> fields, java.util.Map<String,Object> input) {
        FieldMetadata pk = fields.stream().filter(FieldMetadata::isPrimaryKey).findFirst().orElseThrow(() -> new IllegalStateException("No PK"));
        List<String> assigns = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        for (FieldMetadata f : fields) {
            if (f.isPrimaryKey()) continue;
            if (!input.containsKey(f.getFieldName())) continue;
            assigns.add(f.getColumnName() + "=?");
            params.add(input.get(f.getFieldName()));
        }
        params.add(input.get(pk.getFieldName()));
        String sql = "UPDATE " + meta.getTableName() + " SET " + String.join(",", assigns) + " WHERE " + pk.getColumnName() + "=?";
        return new SqlStatement(sql, params);
    }

    public SqlStatement buildSelectById(EntityMetadata meta, Object id) {
        String sql = "SELECT * FROM " + meta.getTableName() + " WHERE " + meta.getPrimaryKeyColumn() + "=?";
        return new SqlStatement(sql, List.of(id));
    }

    public SqlStatement buildDeleteById(EntityMetadata meta, Object id) {
        String sql = "DELETE FROM " + meta.getTableName() + " WHERE " + meta.getPrimaryKeyColumn() + "=?";
        return new SqlStatement(sql, List.of(id));
    }
}
