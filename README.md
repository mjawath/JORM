# Pocket (Lite PocketBase-style) Spring Boot Server

A lightweight Spring Boot 3 + SQLite server that lets you define data collections and expose dynamic REST endpoints (similar in spirit to PocketBase). Endpoint definitions and collection metadata are stored in the SQLite database.

## Features Implemented
- SQLite persistence (single `pocket.db` file by default)
- Collections: create/list/get/delete
- Dynamic endpoints: map arbitrary `GET` and `POST` HTTP paths to a collection
  - `POST` dynamic endpoint: creates a record in the mapped collection (stores arbitrary JSON)
  - `GET` dynamic endpoint: lists all records for the mapped collection
- Records stored as JSON blobs; response flattens object fields into top-level keys
- Basic validation & global exception handling
- Integration test (`PocketIntegrationTest`) exercising end-to-end lifecycle

## Tech Stack
- Java 17
- Spring Boot 3.3.x (Web, JDBC, Validation)
- SQLite JDBC (`org.xerial:sqlite-jdbc`)
- Jackson for JSON
- JUnit 5 for testing

## Project Structure (Key Files)
```
src/main/java/com/example/pocket/
  PocketApplication.java
  db/DatabaseInitializer.java
  model/(CollectionDef, EndpointDef, RecordEntry)
  repository/(CollectionRepository, EndpointRepository, RecordRepository)
  service/(CollectionService, EndpointService, RecordService)
  web/(AdminCollectionController, AdminEndpointController, DynamicEndpointController, GlobalExceptionHandler)
```

## Running the App
Make sure you have Java 17+ and Maven installed (or add the Maven Wrapper).

### 1. Build & Run (with system Maven)
```cmd
mvn spring-boot:run
```
The application creates / uses `pocket.db` in the working directory.

### (Optional) Add Maven Wrapper
If you prefer a wrapper (so users don't need a system-wide Maven), you can generate it:
```cmd
mvn -N wrapper:wrapper -Dmaven=3.9.7
```
Then run:
```cmd
mvnw spring-boot:run
```

## Admin API
| Method | Path                  | Description                         |
|--------|-----------------------|-------------------------------------|
| POST   | /admin/collections    | Create a collection                 |
| GET    | /admin/collections    | List collections                    |
| GET    | /admin/collections/{id}| Get collection by id               |
| DELETE | /admin/collections/{id}| Delete collection                  |
| POST   | /admin/endpoints      | Create dynamic endpoint mapping     |
| GET    | /admin/endpoints      | List endpoint mappings              |

### Create a Collection
```bash
curl -X POST http://localhost:8080/admin/collections \
  -H "Content-Type: application/json" \
  -d '{"name":"tasks"}'
```

### Map Endpoints to a Collection
Create a POST endpoint for creating records:
```bash
curl -X POST http://localhost:8080/admin/endpoints \
  -H "Content-Type: application/json" \
  -d '{"method":"POST","path":"/api/tasks","collectionName":"tasks"}'
```
Create a GET endpoint for listing records:
```bash
curl -X POST http://localhost:8080/admin/endpoints \
  -H "Content-Type: application/json" \
  -d '{"method":"GET","path":"/api/tasks","collectionName":"tasks"}'
```

## Dynamic API Usage
### Create a Record
```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"First task","done":false}'
```
Response example:
```json
{
  "id": "b8f2c6c6-9c2d-4a9e-9d3f-1cf1a2dd2e1b",
  "title": "First task",
  "done": false
}
```

### List Records
```bash
curl http://localhost:8080/api/tasks
```
Response example:
```json
[
  {
    "id": "b8f2c6c6-9c2d-4a9e-9d3f-1cf1a2dd2e1b",
    "title": "First task",
    "done": false
  }
]
```

## Testing
Run all tests:
```cmd
mvn test
```
(If Maven isn't installed, generate the wrapper first or install Maven.)

## Design Notes
- Dynamic routing is handled by broad `@GetMapping("/api/**")` and `@PostMapping("/api/**")` in `DynamicEndpointController`.
- Each request resolves (method, path) to an `EndpointDef`; its `collectionId` is used to find records.
- Future additions (not yet implemented) would include: filtering, pagination, auth, single-record CRUD, schema enforcement, indexing, file storage, migrations.

## Next Steps / Enhancements
1. Add PATCH/PUT/DELETE dynamic record operations
2. Add single-record GET (e.g., `/api/tasks/{id}`)
3. Add schema validation against stored `schema_json`
4. Add simple auth / API keys for admin and dynamic endpoints
5. Pagination & query filtering (limit, offset, sort)
6. Full-text search / indexing strategies
7. Automatic OpenAPI (Swagger) generation from dynamic definitions

## Troubleshooting
| Issue | Cause | Fix |
|-------|-------|-----|
| `mvn` not recognized | Maven not installed | Install Maven or add wrapper |
| SQLite file locked | Concurrent access on Windows | Stop other processes; ensure single running instance |
| 404 on dynamic endpoint | Endpoint not created | Create endpoint via admin API |
| 400 Invalid JSON payload | Malformed JSON request body | Ensure valid JSON |

## License
This project scaffold is provided under the MIT License. Adjust as needed.

