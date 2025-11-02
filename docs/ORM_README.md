# JSON-Based ORM System for SQLite

## Overview

This is a lightweight, configuration-driven Object-Relational Mapping (ORM) system that allows you to persist POJOs (Plain Old Java Objects) to SQLite databases using JSON configuration files. No annotations or code generation required!

## Features

- **JSON Configuration**: Define entity-to-table mappings in simple JSON files
- **Automatic Table Creation**: Tables are created automatically based on JSON configs
- **Full CRUD Operations**: Create, Read, Update, Delete operations without writing SQL
- **Type Conversion**: Automatic conversion between Java types and SQLite types
- **Foreign Key Support**: Define relationships between entities
- **Generic Repository Pattern**: Type-safe repository for any entity

## Quick Start

### 1. Create a JSON Mapping Configuration

Create a JSON file in `src/main/resources/orm/` directory:

```json
{
  "entityClass": "com.mycompany.posswing.model.Customer",
  "tableName": "customer",
  "primaryKey": "id",
  "autoIncrement": true,
  "fields": [
    {
      "javaField": "id",
      "columnName": "id",
      "columnType": "INTEGER",
      "nullable": false
    },
    {
      "javaField": "name",
      "columnName": "name",
      "columnType": "TEXT",
      "nullable": false
    },
    {
      "javaField": "email",
      "columnName": "email",
      "columnType": "TEXT",
      "nullable": true
    }
  ]
}
```

### 2. Initialize the ORM Mapper

```java
JsonOrmMapper mapper = new JsonOrmMapper("myapp.db");
mapper.loadMapping("classpath:orm/customer-mapping.json");
```

### 3. Create a Repository

```java
GenericRepository<Customer> customerRepo = new GenericRepository<>(mapper, Customer.class);
```

### 4. Perform CRUD Operations

```java
// CREATE
Customer customer = new Customer();
customer.setName("John Doe");
customer.setEmail("john@example.com");
int id = customerRepo.save(customer);

// READ
Customer found = customerRepo.findById(id);
List<Customer> all = customerRepo.findAll();

// UPDATE
found.setEmail("newemail@example.com");
customerRepo.update(found);

// DELETE
customerRepo.delete(id);
```

## JSON Configuration Schema

### Root Object

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `entityClass` | String | Yes | Fully qualified class name of the entity |
| `tableName` | String | Yes | Database table name |
| `primaryKey` | String | Yes | Column name of the primary key |
| `autoIncrement` | Boolean | No | Whether primary key auto-increments (default: true) |
| `fields` | Array | Yes | Array of field mappings |

### Field Mapping Object

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `javaField` | String | Yes | Name of the Java field in the entity class |
| `columnName` | String | Yes | Database column name |
| `columnType` | String | Yes | SQLite column type (INTEGER, TEXT, REAL, BLOB) |
| `nullable` | Boolean | No | Whether the column allows NULL values (default: true) |
| `foreignKey` | Object | No | Foreign key constraint definition |

### Foreign Key Object

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `referencedTable` | String | Yes | Name of the referenced table |
| `referencedColumn` | String | Yes | Name of the referenced column |

## SQLite Type Mappings

| Java Type | SQLite Type |
|-----------|-------------|
| Integer, int | INTEGER |
| Long, long | INTEGER |
| String | TEXT |
| Double, double | REAL |
| BigDecimal | REAL |
| Boolean, boolean | INTEGER (0/1) |

## Example: Adding a Foreign Key

```json
{
  "javaField": "customerId",
  "columnName": "customer_id",
  "columnType": "INTEGER",
  "nullable": false,
  "foreignKey": {
    "referencedTable": "customer",
    "referencedColumn": "id"
  }
}
```

## Advanced Usage

### Using in Your Application

```java
public class MyService {
    private final OrmSalesInvoiceRepository repository;
    
    public MyService() throws Exception {
        repository = new OrmSalesInvoiceRepository("myapp.db");
    }
    
    public void createCustomer(String name, String email) throws Exception {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setEmail(email);
        repository.saveCustomer(customer);
    }
    
    public List<Customer> getAllCustomers() throws Exception {
        return repository.findAllCustomers();
    }
}
```

## File Structure

```
src/main/
├── java/com/pos/service/
│   ├── orm/
│   │   ├── EntityMapping.java          # JSON config structure
│   │   ├── JsonOrmMapper.java          # Core ORM engine
│   │   ├── GenericRepository.java      # Generic CRUD repository
│   │   └── OrmDemo.java                # Usage examples
│   ├── OrmSalesInvoiceRepository.java  # Application-specific repository
│   └── SalesInvoiceRepository.java     # Legacy repository (for reference)
└── resources/orm/
    ├── customer-mapping.json           # Customer entity mapping
    └── sku-mapping.json                # SKU entity mapping
```

## Benefits

1. **No Boilerplate Code**: Define mappings once in JSON, use everywhere
2. **Easy Maintenance**: Change table structure by editing JSON, no code changes
3. **Type Safety**: Generic repository provides compile-time type checking
4. **Flexible**: Add new entities by creating JSON configs
5. **Lightweight**: No external ORM dependencies, pure JDBC

## Testing

Run the demo:

```bash
mvn compile exec:java -Dexec.mainClass="com.pos.service.orm.OrmDemo"
```

## Future Enhancements

- Query builder for complex WHERE clauses
- Relationship mapping (OneToMany, ManyToOne)
- Lazy loading
- Transaction support
- Caching layer

