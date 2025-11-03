package com.example.pocket.orm;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PersistenceServiceTest {
    private Connection conn;

    @BeforeEach
    void setup() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement s = conn.createStatement()) {
            s.execute("CREATE TABLE parents(id TEXT PRIMARY KEY, val TEXT)");
            s.execute("CREATE TABLE childs(id TEXT PRIMARY KEY, parent_id TEXT, val TEXT)");
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        conn.close();
    }

    @Test
    void testPersistParentAndChildrenTransactional() throws Exception {
        // metadata
        var parentFields = List.of(
                FieldMetadata.pk("id","id","TEXT"),
                FieldMetadata.regular("val","val","TEXT", false, false)
        );
        var childFields = List.of(
                FieldMetadata.pk("id","id","TEXT"),
                FieldMetadata.fk("parentId","parent_id","TEXT","parents","id"),
                FieldMetadata.regular("val","val","TEXT", false, false)
        );
        var parentMeta = new EntityMetadata("parent","parents","id", parentFields);
        var childMeta = new EntityMetadata("child","childs","id", childFields);
        var metaMap = new java.util.HashMap<String, EntityMetadata>();
        metaMap.put("parent", parentMeta);
        metaMap.put("child", childMeta);

        SqlQueryParser parser = new SqlQueryParser(metaMap);
        PersistenceService svc = new PersistenceService(parser, conn);

        Map<String,Object> p = new HashMap<>();
        p.put("val","P1");
        Map<String,Object> c1 = new HashMap<>(); c1.put("val","C1");
        Map<String,Object> c2 = new HashMap<>(); c2.put("val","C2");
        p.put("child", List.of(c1, c2));

        ExecutionResult res = svc.persist("parent", p);
        assertTrue(res.getAffectedRows() >= 3);
        assertEquals(3, res.getGeneratedKeys().size());
    }
}
