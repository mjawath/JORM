package com.example.pocket.repository;

import com.example.pocket.model.EndpointDef;
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
public class EndpointRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<EndpointDef> rowMapper = (rs, rn) -> new EndpointDef(
            rs.getLong("id"),
            rs.getString("method"),
            rs.getString("path"),
            rs.getLong("collection_id"),
            Instant.parse(rs.getString("created_at")),
            Instant.parse(rs.getString("updated_at"))
    );

    public EndpointRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public EndpointDef create(String method, String path, Long collectionId) {
        Instant now = Instant.now();
        KeyHolder kh = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO endpoints(method, path, collection_id, created_at, updated_at) VALUES(?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, method);
            ps.setString(2, path);
            ps.setLong(3, collectionId);
            ps.setString(4, now.toString());
            ps.setString(5, now.toString());
            return ps;
        }, kh);
        // Prefer getKey() which returns the generated key in a driver-independent way.
        Number key = kh.getKey();
        Long id;
        if (key != null) {
            id = key.longValue();
        } else if (kh.getKeys() != null && kh.getKeys().get("id") instanceof Number) {
            id = ((Number) kh.getKeys().get("id")).longValue();
        } else {
            // As a last resort, query the last_insert_rowid() for SQLite compatibility
            Long last = jdbcTemplate.queryForObject("SELECT last_insert_rowid()", Long.class);
            id = last;
        }
        return new EndpointDef(id, method, path, collectionId, now, now);
    }

    public Optional<EndpointDef> findByMethodAndPath(String method, String path) {
        return jdbcTemplate.query("SELECT * FROM endpoints WHERE method=? AND path=?", rowMapper, method, path).stream().findFirst();
    }

    public List<EndpointDef> findAll() { return jdbcTemplate.query("SELECT * FROM endpoints ORDER BY id", rowMapper); }

    public boolean delete(Long id) { return jdbcTemplate.update("DELETE FROM endpoints WHERE id=?", id) > 0; }
}

