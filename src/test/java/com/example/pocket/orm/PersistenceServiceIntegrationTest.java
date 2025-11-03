package com.example.pocket.orm;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PersistenceServiceIntegrationTest {
    private Connection conn;

    @BeforeEach
    void setup() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement s = conn.createStatement()) {
            s.execute("CREATE TABLE parents(id TEXT PRIMARY KEY, name TEXT)");
            s.execute("CREATE TABLE children(id TEXT PRIMARY KEY, parent_id TEXT, name TEXT)");
        }
    }

    @AfterEach
    void teardown() throws Exception {
        conn.close();
    }

    @Test
    void persistParentWithChildren_commitsAndPropagatesFk() throws Exception {
        // build metadata
        var parentFields = List.of(
                FieldMetadata.pk("id","id","TEXT"),
                FieldMetadata.regular("name","name","TEXT", false, false)
        );
        var childFields = List.of(
                FieldMetadata.pk("id","id","TEXT"),
                FieldMetadata.fk("parentId","parent_id","TEXT","parents","id"),
                FieldMetadata.regular("name","name","TEXT", false, false)
        );
        var parentMeta = new EntityMetadata("parent","parents","id", parentFields);
        var childMeta = new EntityMetadata("child","children","id", childFields);
        var metaMap = new HashMap<String, EntityMetadata>();
        metaMap.put("parent", parentMeta);
        metaMap.put("child", childMeta);

        SqlQueryParser parser = new SqlQueryParser(metaMap);
        PersistenceService svc = new PersistenceService(parser, conn);

        Map<String,Object> parent = new HashMap<>();
        parent.put("name", "p1");
        Map<String,Object> c1 = new HashMap<>(); c1.put("name","c1");
        Map<String,Object> c2 = new HashMap<>(); c2.put("name","c2");
        parent.put("child", List.of(c1, c2));

        ExecutionResult res = svc.persist("parent", parent);
        assertTrue(res.getAffectedRows() >= 3);
        assertEquals(3, res.getGeneratedKeys().size());

        // Verify rows in DB and FK values
        try (PreparedStatement ps = conn.prepareStatement("SELECT id, name FROM parents")) {
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
                String pid = rs.getString("id");
                assertEquals("p1", rs.getString("name"));
                // children
                try (PreparedStatement ps2 = conn.prepareStatement("SELECT parent_id, name FROM children ORDER BY name")) {
                    try (ResultSet rs2 = ps2.executeQuery()) {
                        assertTrue(rs2.next());
                        assertEquals(pid, rs2.getString("parent_id"));
                        assertEquals("c1", rs2.getString("name"));
                        assertTrue(rs2.next());
                        assertEquals(pid, rs2.getString("parent_id"));
                        assertEquals("c2", rs2.getString("name"));
                    }
                }
            }
        }
    }
}
