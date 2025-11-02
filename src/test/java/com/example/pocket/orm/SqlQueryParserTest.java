package com.example.pocket.orm;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class SqlQueryParserTest {

    private SqlQueryParser createParser() {
        List<FieldMetadata> customerFields = List.of(
                FieldMetadata.pk("id","id","TEXT"),
                FieldMetadata.regular("name","name","TEXT", false, false),
                FieldMetadata.regular("email","email","TEXT", true, true)
        );
        EntityMetadata customer = new EntityMetadata("customer","customers","id", customerFields);
        Map<String, EntityMetadata> map = new HashMap<>();
        map.put("customer", customer);
        return new SqlQueryParser(map);
    }

    @Test
    void testInsertBuildsAllColumnsAndGeneratesPk() {
        SqlQueryParser parser = createParser();
        Map<String,Object> data = new HashMap<>();
        data.put("name","Alice");
        data.put("email","a@example.com");
        SqlQueryParser.ParsedSql ps = parser.buildInsertFromMap("customer", data);
        assertEquals("INSERT INTO customers (id,name,email) VALUES (?,?,?)", ps.getSql());
        assertEquals(3, ps.getParameters().size());
        assertNotNull(data.get("id")); // PK generated
    }

    @Test
    void testUpdatePartial() {
        SqlQueryParser parser = createParser();
        Map<String,Object> data = new HashMap<>();
        data.put("id","123");
        data.put("name","Bob");
        SqlQueryParser.ParsedSql ps = parser.buildUpdateFromMap("customer", data);
        assertEquals("UPDATE customers SET name=? WHERE id=?", ps.getSql());
        assertEquals(Arrays.asList("Bob","123"), ps.getParameters());
    }

    @Test
    void testSelectFilters() {
        SqlQueryParser parser = createParser();
        Map<String,Object> filters = new HashMap<>();
        filters.put("name","Charlie");
        SqlQueryParser.ParsedSql ps = parser.buildSelectFromFilters("customer", filters);
        assertEquals("SELECT * FROM customers WHERE name=?", ps.getSql());
        assertEquals(List.of("Charlie"), ps.getParameters());
    }

    @Test
    void testDelete() {
        SqlQueryParser parser = createParser();
        SqlQueryParser.ParsedSql ps = parser.buildDeleteById("customer", "999");
        assertEquals("DELETE FROM customers WHERE id=?", ps.getSql());
        assertEquals(List.of("999"), ps.getParameters());
    }
}
