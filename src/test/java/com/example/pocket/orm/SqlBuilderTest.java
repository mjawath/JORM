package com.example.pocket.orm;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SqlBuilderTest {

    @Test
    void buildInsertGeneratesSqlAndParams() {
        FieldMetadata f1 = FieldMetadata.pk("id","id","TEXT");
        FieldMetadata f2 = FieldMetadata.regular("name","name","TEXT", false, false);
        EntityMetadata em = new EntityMetadata("customer","customers","id", List.of(f1,f2));
        SqlBuilder b = new SqlBuilder();
        var stmt = b.buildInsert(em, em.getFields(), Map.of("id","42","name","Alice"));
        assertEquals("INSERT INTO customers (id,name) VALUES (?,?)", stmt.getSql());
        assertEquals(List.of("42","Alice"), stmt.getParameters());
    }

    @Test
    void buildUpdateGeneratesSqlAndParams() {
        FieldMetadata f1 = FieldMetadata.pk("id","id","TEXT");
        FieldMetadata f2 = FieldMetadata.regular("name","name","TEXT", false, false);
        EntityMetadata em = new EntityMetadata("customer","customers","id", List.of(f1,f2));
        SqlBuilder b = new SqlBuilder();
        var stmt = b.buildUpdate(em, em.getFields(), Map.of("id","42","name","Bob"));
        assertEquals("UPDATE customers SET name=? WHERE id=?", stmt.getSql());
        assertEquals(List.of("Bob","42"), stmt.getParameters());
    }
}
