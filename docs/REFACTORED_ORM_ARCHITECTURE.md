# Refactored ORM Architecture

## Overview

This refactored ORM implementation provides a clean separation of concerns with the following architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                    RefactoredOrmMapper                       │
│                      (Main Facade)                           │
└─────────────────────────────────────────────────────────────┘
                              │
                ┌─────────────┼─────────────┐
                │             │             │
                ▼             ▼             ▼
        ┌───────────┐  ┌───────────┐  ┌───────────┐
        │  Parser   │  │   SQL     │  │ Executor  │
        │           │  │  Builder  │  │           │
        └───────────┘  └───────────┘  └───────────┘
                │             │             │
                ▼             ▼             ▼
        ┌───────────┐  ┌───────────┐  ┌───────────┐
        │ Metamodel │  │   SQL     │  │Execution  │
        │           │  │ Statement │  │   Plan    │
        └───────────┘  └───────────┘  └───────────┘
                                            │
                                            ▼
                                      ┌───────────┐
                                      │Converter  │
                                      │           │
                                      └───────────┘
```

## Components

### 1. Parser/Mapper (`parser` package)

**Responsibility**: Convert JSON configuration to runtime metamodel

- `MetamodelParser`: Parses JSON files and creates EntityMetamodel objects
- Loads class definitions via reflection
- Caches parsed metamodels

**Example**:
```java
MetamodelParser parser = new MetamodelParser();
EntityMetamodel metamodel = parser.parse("classpath:orm/customer-mapping.json");
```

### 2. Metamodel (`metamodel` package)

**Responsibility**: Runtime representation of entity structure

- `EntityMetamodel`: Complete entity definition with fields, primary key, relationships
- `FieldMetamodel`: Individual field/column mapping
- `SqlType`: Type system mapping Java types to SQL types
- `ForeignKeyMetamodel`: Foreign key relationships
- `RelationshipMetamodel`: One-to-many, many-to-one relationships

**Example**:
```java
EntityMetamodel metamodel = EntityMetamodel.builder()
    .entityClass(Customer.class)
    .tableName("customers")
    .addField(field1)
    .primaryKey(idField)
    .build();
```

### 3. SQL Builder (`sql` package)

**Responsibility**: Generate SQL statements from metamodel

- `SqlBuilder`: Creates SQL for CRUD operations
- `SqlStatement`: SQL string + parameter metadata
- `WhereClause`: Fluent API for WHERE conditions

**Example**:
```java
SqlBuilder builder = new SqlBuilder();
SqlStatement insertSql = builder.buildInsert(metamodel);
SqlStatement selectSql = builder.buildSelectById(metamodel);

WhereClause where = WhereClause.create()
    .equals("status", "active")
    .greaterThan("price", 10.0);
SqlStatement customQuery = builder.buildSelectWithWhere(metamodel, where);
```

### 4. SQL Executor (`executor` package)

**Responsibility**: Execute SQL statements with monitoring

- `SqlExecutor`: Executes prepared statements
- `ExecutionPlan`: Tracks execution statistics
- `ExecutionResult`: Result of INSERT/UPDATE/DELETE
- `QueryResult`: Result of SELECT queries
- `ParameterBinder`: Binds Java values to SQL parameters

**Features**:
- Execution time tracking
- Affected rows counting
- Error handling
- Transaction support
- Generated key retrieval

**Example**:
```java
SqlExecutor executor = new SqlExecutor("jdbc:sqlite:database.db");
ExecutionResult result = executor.executeInsert(statement, entity);
System.out.println("Execution time: " + result.getExecutionTime() + "ms");
System.out.println("Generated key: " + result.getGeneratedKey());
```

### 5. Result Converter (`converter` package)

**Responsibility**: Convert SQL results to Java objects

- `EntityConverter`: Maps ResultSet to entity instances
- `ResultSetConverter`: Functional interface for custom conversions
- Automatic type conversion (SQL → Java)

**Example**:
```java
EntityConverter converter = new EntityConverter();
Customer customer = converter.convertToEntity(resultSet, metamodel, Customer.class);
```

## Usage

### Basic CRUD Operations

```java
// 1. Initialize ORM
RefactoredOrmMapper orm = new RefactoredOrmMapper("database.db");

// 2. Load mappings
orm.loadMapping("classpath:orm/customer-mapping.json");
orm.loadMapping("classpath:orm/sku-mapping.json");

// 3. Insert
Customer customer = new Customer();
customer.setName("John Doe");
int id = orm.insert(customer);

// 4. Find by ID
Customer found = orm.findById(Customer.class, id);

// 5. Find all
List<Customer> all = orm.findAll(Customer.class);

// 6. Update
found.setName("Jane Doe");
orm.update(found);

// 7. Delete
orm.delete(Customer.class, id);
```

### Advanced Queries

```java
// WHERE clause queries
WhereClause where = WhereClause.create()
    .like("name", "%John%")
    .greaterThan("age", 18);

List<Customer> results = orm.findWhere(Customer.class, where);

// Complex conditions
WhereClause complex = WhereClause.create()
    .openParenthesis()
        .equals("status", "active")
        .or("priority", "=", "high")
    .closeParenthesis()
    .greaterThan("created_date", someDate);
```

### Transactions

```java
orm.executeInTransaction(conn -> {
    // Multiple operations in single transaction
    int id1 = orm.insert(entity1);
    int id2 = orm.insert(entity2);
    orm.update(entity3);
    return id1;
});
```

## Benefits of This Architecture

### 1. **Separation of Concerns**
Each component has a single, well-defined responsibility:
- Parser knows about JSON and reflection
- SQL Builder knows about SQL syntax
- Executor knows about JDBC and connections
- Converter knows about type mapping

### 2. **Testability**
Each component can be tested independently:
```java
// Test SQL builder without database
SqlBuilder builder = new SqlBuilder();
SqlStatement sql = builder.buildInsert(mockMetamodel);
assertEquals("INSERT INTO ...", sql.getSql());
```

### 3. **Flexibility**
Components can be swapped or extended:
- Use different parsers (XML, annotations, code)
- Support different databases (PostgreSQL, MySQL)
- Custom type converters
- Custom execution strategies

### 4. **Performance Monitoring**
ExecutionPlan tracks every query:
```java
ExecutionResult result = orm.insert(entity);
System.out.println(result.getExecutionPlan());
// Output:
// ExecutionPlan{
//   sql='INSERT INTO customers ...'
//   executionTime=5ms
//   affectedRows=1
// }
```

### 5. **Type Safety**
Metamodel provides compile-time safety:
```java
FieldMetamodel field = metamodel.getField("name").orElseThrow();
SqlType type = field.getSqlType(); // Type-safe enum
```

### 6. **Extensibility**
Easy to add features:
- Caching layer between executor and database
- Query optimization hints
- Batch operations
- Lazy loading for relationships
- Second-level cache

## Implementation Details

### How It Works

1. **Loading Phase**:
   ```
   JSON Config → Parser → Metamodel → SQL Builder → CREATE TABLE → Executor
   ```

2. **Insert Operation**:
   ```
   Entity → Metamodel → SQL Builder → SqlStatement → Executor → ParameterBinder → DB
                                                                        ↓
                                                                  ExecutionPlan
   ```

3. **Query Operation**:
   ```
   Class → Metamodel → SQL Builder → SqlStatement → Executor → ResultSet → Converter → Entity
                                                                     ↓
                                                               ExecutionPlan
   ```

### Key Design Patterns

- **Facade Pattern**: RefactoredOrmMapper provides simple interface
- **Builder Pattern**: Metamodel, WhereClause use builders
- **Strategy Pattern**: ResultSetConverter allows custom conversions
- **Template Method**: SqlExecutor defines execution flow

## Future Enhancements

1. **Query Cache**: Cache parsed WHERE clauses
2. **Prepared Statement Pool**: Reuse prepared statements
3. **Batch Operations**: Insert/update multiple entities efficiently
4. **Relationship Loading**: Automatic join generation for foreign keys
5. **Schema Migration**: Track and apply schema changes
6. **Index Management**: Create/manage indexes from config
7. **Connection Pooling**: Pool database connections
8. **Async Operations**: Non-blocking database operations

## Comparison with Old Implementation

| Aspect | Old (JsonOrmMapper) | New (RefactoredOrmMapper) |
|--------|---------------------|---------------------------|
| Structure | Monolithic class | Separated components |
| Testability | Hard to test | Easy to unit test |
| SQL Generation | Inline string building | Dedicated SQL builder |
| Execution Tracking | None | Full execution plans |
| Type Safety | Runtime only | Compile-time metamodel |
| Extensibility | Difficult | Easy to extend |
| Error Handling | Basic | Detailed with plans |
| Performance Monitoring | None | Built-in |

## Migration Guide

To migrate from old JsonOrmMapper:

```java
// Old way
JsonOrmMapper oldOrm = new JsonOrmMapper("database.db");
oldOrm.loadMapping("classpath:orm/customer-mapping.json");
int id = oldOrm.insert(customer);

// New way
RefactoredOrmMapper newOrm = new RefactoredOrmMapper("database.db");
newOrm.loadMapping("classpath:orm/customer-mapping.json");
int id = newOrm.insert(customer);
```

The API is backward compatible for basic operations!
package com.pos.service.orm;

import com.mycompany.posswing.model.Customer;
import com.mycompany.posswing.model.Sku;
import com.pos.service.orm.sql.WhereClause;

import java.math.BigDecimal;
import java.util.List;

/**
 * Demonstrates the refactored ORM with separated concerns:
 * 1. Parser/Mapper: Parses JSON config to metamodel
 * 2. SQL Builder: Builds SQL statements
 * 3. Executor: Executes SQL with execution plans
 * 4. Converter: Converts results to entities
 */
public class RefactoredOrmDemo {

    public static void main(String[] args) {
        try {
            // Initialize the ORM
            RefactoredOrmMapper orm = new RefactoredOrmMapper("pos_refactored.db");
            
            // Load entity mappings
            System.out.println("=== Loading Entity Mappings ===");
            orm.loadMapping("classpath:orm/customer-mapping.json");
            orm.loadMapping("classpath:orm/sku-mapping.json");
            
            System.out.println("Metamodel Registry: " + orm.getMetamodelRegistry().keySet());
            System.out.println();

            // Test Customer operations
            testCustomerOperations(orm);
            
            // Test SKU operations
            testSkuOperations(orm);
            
            // Test WHERE clause queries
            testWhereClauseQueries(orm);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void testCustomerOperations(RefactoredOrmMapper orm) throws Exception {
        System.out.println("=== Testing Customer Operations ===");
        
        // Create and insert a customer
        Customer customer = new Customer();
        customer.setName("John Doe");
        customer.setEmail("john@example.com");
        customer.setPhone("123-456-7890");
        customer.setAddress("123 Main St");
        
        int customerId = orm.insert(customer);
        System.out.println("Inserted customer with ID: " + customerId);
        
        // Find by ID
        Customer found = orm.findById(Customer.class, customerId);
        System.out.println("Found customer: " + found.getName() + " - " + found.getEmail());
        
        // Update customer
        found.setEmail("john.doe@example.com");
        orm.update(found);
        System.out.println("Updated customer email");
        
        // Find all customers
        List<Customer> allCustomers = orm.findAll(Customer.class);
        System.out.println("Total customers: " + allCustomers.size());
        System.out.println();
    }

    private static void testSkuOperations(RefactoredOrmMapper orm) throws Exception {
        System.out.println("=== Testing SKU Operations ===");
        
        // Create and insert SKUs
        Sku sku1 = new Sku();
        sku1.setCode("SKU001");
        sku1.setDescription("Product 1");
        sku1.setPrice(BigDecimal.valueOf(19.99));
        sku1.setStockQuantity(100);
        
        int sku1Id = orm.insert(sku1);
        System.out.println("Inserted SKU with ID: " + sku1Id);
        
        Sku sku2 = new Sku();
        sku2.setCode("SKU002");
        sku2.setDescription("Product 2");
        sku2.setPrice(BigDecimal.valueOf(29.99));
        sku2.setStockQuantity(50);
        
        int sku2Id = orm.insert(sku2);
        System.out.println("Inserted SKU with ID: " + sku2Id);
        
        // Find all SKUs
        List<Sku> allSkus = orm.findAll(Sku.class);
        System.out.println("Total SKUs: " + allSkus.size());
        for (Sku sku : allSkus) {
            System.out.println("  - " + sku.getCode() + ": " + sku.getDescription() + " @ $" + sku.getPrice());
        }
        System.out.println();
    }

    private static void testWhereClauseQueries(RefactoredOrmMapper orm) throws Exception {
        System.out.println("=== Testing WHERE Clause Queries ===");
        
        // Find customers by email pattern
        WhereClause whereClause = WhereClause.create()
                .like("email", "%example.com%");
        
        List<Customer> customers = orm.findWhere(Customer.class, whereClause);
        System.out.println("Customers with example.com email: " + customers.size());
        
        // Find SKUs with price greater than 20
        WhereClause priceClause = WhereClause.create()
                .greaterThan("price", 20.0);
        
        List<Sku> expensiveSkus = orm.findWhere(Sku.class, priceClause);
        System.out.println("SKUs with price > $20: " + expensiveSkus.size());
        
        // Complex WHERE clause
        WhereClause complexClause = WhereClause.create()
                .greaterThan("price", 10.0)
                .and("stock_quantity", ">", 0);
        
        List<Sku> availableSkus = orm.findWhere(Sku.class, complexClause);
        System.out.println("Available SKUs (price > $10 and in stock): " + availableSkus.size());
        System.out.println();
    }
}

