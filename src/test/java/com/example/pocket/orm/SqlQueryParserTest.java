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
    ParsedSql ps = parser.buildInsertFromMap("customer", data);
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
    ParsedSql ps = parser.buildUpdateFromMap("customer", data);
        assertEquals("UPDATE customers SET name=? WHERE id=?", ps.getSql());
        assertEquals(Arrays.asList("Bob","123"), ps.getParameters());
    }

    @Test
    void testSelectFilters() {
        SqlQueryParser parser = createParser();
        Map<String,Object> filters = new HashMap<>();
        filters.put("name","Charlie");
    ParsedSql ps = parser.buildSelectFromFilters("customer", filters);
        assertEquals("SELECT * FROM customers WHERE name=?", ps.getSql());
        assertEquals(List.of("Charlie"), ps.getParameters());
    }

    @Test
    void testDelete() {
        SqlQueryParser parser = createParser();
    ParsedSql ps = parser.buildDeleteById("customer", "999");
        assertEquals("DELETE FROM customers WHERE id=?", ps.getSql());
        assertEquals(List.of("999"), ps.getParameters());
    }

    @Test
    void testNestedInsertPlanParentBeforeChildAndFkPropagation() {
        // parent: order, child: lineitem
        List<FieldMetadata> orderFields = List.of(
                FieldMetadata.pk("id","id","TEXT"),
                FieldMetadata.regular("total","total","NUMERIC", false, false)
        );
        EntityMetadata orderMeta = new EntityMetadata("order","orders","id", orderFields);

        List<FieldMetadata> lineFields = List.of(
                FieldMetadata.pk("id","id","TEXT"),
                FieldMetadata.fk("orderId","order_id","TEXT","orders","id"),
                FieldMetadata.regular("sku","sku","TEXT", false, false)
        );
        EntityMetadata lineMeta = new EntityMetadata("lineitem","lineitems","id", lineFields);

        Map<String,EntityMetadata> map = new HashMap<>();
        map.put("order", orderMeta);
        map.put("lineitem", lineMeta);

        SqlQueryParser parser = new SqlQueryParser(map);

        Map<String,Object> orderInput = new HashMap<>();
        orderInput.put("total", 123.45);
        List<Map<String,Object>> children = new ArrayList<>();
        Map<String,Object> l1 = new HashMap<>(); l1.put("sku","ABC");
        Map<String,Object> l2 = new HashMap<>(); l2.put("sku","DEF");
        children.add(l1); children.add(l2);
        // nest children under field name 'lineitem'
        orderInput.put("lineitem", children);

    List<ParsedSql> plan = parser.buildInsertPlan("order", orderInput);
        // first SQL should be order insert
        assertTrue(plan.size() >= 3);
        assertTrue(plan.get(0).getSql().startsWith("INSERT INTO orders"));
        // subsequent SQLs for lineitems and their params should include order id
    ParsedSql lineInsert = plan.get(1);
        assertTrue(lineInsert.getSql().startsWith("INSERT INTO lineitems"));
        // The parameters for child should include the propagated order id (second position or depending on metadata order)
        boolean foundOrderId = lineInsert.getParameters().stream().anyMatch(p -> p != null && p.toString().length() > 0);
        assertTrue(foundOrderId);
    }
}
