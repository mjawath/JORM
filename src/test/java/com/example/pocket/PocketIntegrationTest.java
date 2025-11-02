package com.example.pocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PocketIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void endToEnd_dynamicEndpointLifecycle() throws Exception {
        // 1. Create collection
        Map<String, Object> collReq = new HashMap<>();
        collReq.put("name", "tasks");
        ResponseEntity<String> collResp = restTemplate.postForEntity("/admin/collections", collReq, String.class);
        assertThat(collResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode collJson = mapper.readTree(collResp.getBody());
        long collectionId = collJson.get("id").asLong();
        assertThat(collJson.get("name").asText()).isEqualTo("tasks");

        // 2. Create POST endpoint mapping
        Map<String, Object> epPostReq = new HashMap<>();
        epPostReq.put("method", "POST");
        epPostReq.put("path", "/api/tasks");
        epPostReq.put("collectionName", "tasks");
        ResponseEntity<String> epPostResp = restTemplate.postForEntity("/admin/endpoints", epPostReq, String.class);
        assertThat(epPostResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 3. Create GET endpoint mapping
        Map<String, Object> epGetReq = new HashMap<>();
        epGetReq.put("method", "GET");
        epGetReq.put("path", "/api/tasks");
        epGetReq.put("collectionName", "tasks");
        ResponseEntity<String> epGetResp = restTemplate.postForEntity("/admin/endpoints", epGetReq, String.class);
        assertThat(epGetResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // 4. POST a record via dynamic endpoint
        Map<String, Object> recordReq = new HashMap<>();
        recordReq.put("title", "First task");
        recordReq.put("done", false);
        ResponseEntity<String> recordResp = restTemplate.postForEntity("/api/tasks", recordReq, String.class);
        assertThat(recordResp.getStatusCode()).isEqualTo(HttpStatus.OK); // dynamic controller returns 200
        JsonNode recordJson = mapper.readTree(recordResp.getBody());
        assertThat(recordJson.get("title").asText()).isEqualTo("First task");
        assertThat(recordJson.get("id").asText()).isNotEmpty();

        // 5. GET list via dynamic endpoint
        ResponseEntity<String> listResp = restTemplate.getForEntity("/api/tasks", String.class);
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode listNode = mapper.readTree(listResp.getBody());
        assertThat(listNode.isArray()).isTrue();
        assertThat(listNode.size()).isGreaterThanOrEqualTo(1);
        boolean found = false;
        for (JsonNode n : listNode) {
            if (n.has("title") && n.get("title").asText().equals("First task")) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }

    @Test
    void schemaValidation_dynamicEndpoint() throws Exception {
        // 1. Create collection with schema (title: string, done: boolean required)
        String schema = "{"
            + "  \"type\": \"object\","
            + "  \"properties\": {"
            + "    \"title\": {\"type\": \"string\"},"
            + "    \"done\": {\"type\": \"boolean\"}"
            + "  },"
            + "  \"required\": [\"title\", \"done\"]"
            + "}";
        Map<String, Object> collReq = new HashMap<>();
        collReq.put("name", "tasks_schema");
        collReq.put("schemaJson", schema);
        ResponseEntity<String> collResp = restTemplate.postForEntity("/admin/collections", collReq, String.class);
        assertThat(collResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode collJson = mapper.readTree(collResp.getBody());
        long collectionId = collJson.get("id").asLong();

        // 2. Create POST and GET endpoint mappings
        Map<String, Object> epPostReq = new HashMap<>();
        epPostReq.put("method", "POST");
        epPostReq.put("path", "/api/tasks_schema");
        epPostReq.put("collectionName", "tasks_schema");
        restTemplate.postForEntity("/admin/endpoints", epPostReq, String.class);
        Map<String, Object> epGetReq = new HashMap<>();
        epGetReq.put("method", "GET");
        epGetReq.put("path", "/api/tasks_schema");
        epGetReq.put("collectionName", "tasks_schema");
        restTemplate.postForEntity("/admin/endpoints", epGetReq, String.class);

        // 3. POST a valid record
        Map<String, Object> validRecord = new HashMap<>();
        validRecord.put("title", "Task 1");
        validRecord.put("done", false);
        ResponseEntity<String> validResp = restTemplate.postForEntity("/api/tasks_schema", validRecord, String.class);
        assertThat(validResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode validJson = mapper.readTree(validResp.getBody());
        assertThat(validJson.get("title").asText()).isEqualTo("Task 1");

        // 4. POST an invalid record (missing required field)
        Map<String, Object> invalidRecord = new HashMap<>();
        invalidRecord.put("title", "Task 2");
        ResponseEntity<String> invalidResp = restTemplate.postForEntity("/api/tasks_schema", invalidRecord, String.class);
        assertThat(invalidResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // 5. POST a record with invalid type (done as string)
        Map<String, Object> invalidTypeRecord = new HashMap<>();
        invalidTypeRecord.put("title", "Task 3");
        invalidTypeRecord.put("done", "not_a_boolean");
        ResponseEntity<String> invalidTypeResp = restTemplate.postForEntity("/api/tasks_schema", invalidTypeRecord, String.class);
        assertThat(invalidTypeResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // 6. POST a record with null for required field
        Map<String, Object> nullFieldRecord = new HashMap<>();
        nullFieldRecord.put("title", null);
        nullFieldRecord.put("done", false);
        ResponseEntity<String> nullFieldResp = restTemplate.postForEntity("/api/tasks_schema", nullFieldRecord, String.class);
        assertThat(nullFieldResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // 7. POST a record with extra field (should be allowed by default)
        Map<String, Object> extraFieldRecord = new HashMap<>();
        extraFieldRecord.put("title", "Task 4");
        extraFieldRecord.put("done", true);
        extraFieldRecord.put("extra", 123);
        ResponseEntity<String> extraFieldResp = restTemplate.postForEntity("/api/tasks_schema", extraFieldRecord, String.class);
        assertThat(extraFieldResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode extraFieldJson = mapper.readTree(extraFieldResp.getBody());
        assertThat(extraFieldJson.get("title").asText()).isEqualTo("Task 4");
        assertThat(extraFieldJson.get("extra").asInt()).isEqualTo(123);

        // 8. POST a record with all fields valid
        Map<String, Object> allFieldsRecord = new HashMap<>();
        allFieldsRecord.put("title", "Task 5");
        allFieldsRecord.put("done", true);
        ResponseEntity<String> allFieldsResp = restTemplate.postForEntity("/api/tasks_schema", allFieldsRecord, String.class);
        assertThat(allFieldsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode allFieldsJson = mapper.readTree(allFieldsResp.getBody());
        assertThat(allFieldsJson.get("title").asText()).isEqualTo("Task 5");
        assertThat(allFieldsJson.get("done").asBoolean()).isTrue();
    }
}
