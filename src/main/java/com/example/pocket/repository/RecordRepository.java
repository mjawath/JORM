package com.example.pocket.repository;

import com.example.pocket.model.RecordEntry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RecordRepository {
    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<RecordEntry> rowMapper = (rs, rn) -> new RecordEntry(
            rs.getString("id"),
            rs.getLong("collection_id"),
            rs.getString("data_json"),
            Instant.parse(rs.getString("created_at")),
            Instant.parse(rs.getString("updated_at"))
    );

    public RecordRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

    public RecordEntry create(Long collectionId, String dataJson) {
        Instant now = Instant.now();
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("INSERT INTO records(id, collection_id, data_json, created_at, updated_at) VALUES(?,?,?,?,?)",
                id, collectionId, dataJson, now.toString(), now.toString());
        return new RecordEntry(id, collectionId, dataJson, now, now);
    }

    public List<RecordEntry> listByCollection(Long collectionId) {
        return jdbcTemplate.query("SELECT * FROM records WHERE collection_id=? ORDER BY created_at", rowMapper, collectionId);
    }

    public Optional<RecordEntry> findById(String id) {
        return jdbcTemplate.query("SELECT * FROM records WHERE id=?", rowMapper, id).stream().findFirst();
    }
}

