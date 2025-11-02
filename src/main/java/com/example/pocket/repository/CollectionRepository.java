package com.example.pocket.repository;

import com.example.pocket.model.CollectionDef;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class CollectionRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<CollectionDef> rowMapper = (rs, rn) -> new CollectionDef(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("schema_json"),
            Instant.parse(rs.getString("created_at")),
            Instant.parse(rs.getString("updated_at"))
    );

    public CollectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CollectionDef create(String name, String schemaJson) {
        Instant now = Instant.now();
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO collections(name, schema_json, created_at, updated_at) VALUES(?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, name);
            ps.setString(2, schemaJson);
            ps.setString(3, now.toString());
            ps.setString(4, now.toString());
            return ps;
        }, kh);
        Long id = kh.getKey().longValue();
        return new CollectionDef(id, name, schemaJson, now, now);
    }

    public List<CollectionDef> findAll() { return jdbcTemplate.query("SELECT * FROM collections ORDER BY id", rowMapper); }

    public Optional<CollectionDef> findById(Long id) { return jdbcTemplate.query("SELECT * FROM collections WHERE id=?", rowMapper, id).stream().findFirst(); }

    public Optional<CollectionDef> findByName(String name) { return jdbcTemplate.query("SELECT * FROM collections WHERE name=?", rowMapper, name).stream().findFirst(); }

    public boolean delete(Long id) { return jdbcTemplate.update("DELETE FROM collections WHERE id=?", id) > 0; }
}
