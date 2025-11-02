# Operation Orchestration Layer

## Overview

The **OperationOrchestrator** is a key component that coordinates the complete execution of database operations, providing a clean workflow with well-defined steps.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                  RefactoredOrmMapper (Facade)                │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│               OperationOrchestrator (Coordinator)            │
│                                                              │
│  Workflow for each operation:                               │
│  1. ✓ Identify operation type (INSERT, SELECT, etc.)        │
│  2. ✓ Correlate entity with metamodel                       │
│  3. ✓ Prepare SQL statement inputs                          │
│  4. ✓ Execute SQL                                           │
│  5. ✓ Convert results                                       │
└─────────────────────────────────────────────────────────────┘
         │              │              │              │
         ▼              ▼              ▼              ▼
┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐
│ Metamodel  │  │    SQL     │  │  Executor  │  │ Converter  │
│ Correlator │  │  Builder   │  │            │  │            │
└────────────┘  └────────────┘  └────────────┘  └────────────┘
```

## Components

### 1. OperationRequest

Represents a request to perform a database operation with all necessary context.

```java
OperationRequest request = OperationRequest.builder()
    .operationType(OperationType.INSERT)
    .entity(customer)
    .metamodel(customerMetamodel)
    .build();
```

**Properties:**
- `operationType` - INSERT, SELECT_BY_ID, SELECT_ALL, SELECT_WHERE, UPDATE, DELETE
- `entity` - The entity object
- `entityClass` - Class type
- `metamodel` - EntityMetamodel from registry
- `parameters` - Additional parameters (e.g., WHERE clause)
- `primaryKeyValue` - For SELECT_BY_ID and DELETE

### 2. OperationResult

Contains the result of an orchestrated operation with execution statistics.

```java
OperationResult<Customer> result = orchestrator.orchestrate(request);

if (result.isSuccess()) {
    Customer customer = result.getResult();
    int generatedKey = result.getGeneratedKey();
    ExecutionPlan plan = result.getExecutionPlan();
    System.out.println("Execution time: " + plan.getExecutionTime() + "ms");
}
```

**Properties:**
- `success` - Operation success status
- `result` - Single entity result
- `resultList` - List of entities
- `generatedKey` - Auto-generated primary key
- `affectedRows` - Number of rows affected
- `executionPlan` - Detailed execution statistics
- `error` - Exception if operation failed

### 3. EntityMetamodelCorrelator

Correlates entity objects with their metamodel definitions and extracts values.

```java
EntityMetamodelCorrelator correlator = new EntityMetamodelCorrelator();

// Extract all field values
CorrelationResult correlation = correlator.correlate(customer, metamodel);
Map<FieldMetamodel, Object> fieldValues = correlation.getFieldValues();
Map<String, Object> columnValues = correlation.getColumnValues();

// Extract insertable values (excludes auto-increment PK)
Map<FieldMetamodel, Object> insertableValues = 
    correlator.extractInsertableValues(customer, metamodel);

// Extract updatable values (excludes PK)
Map<FieldMetamodel, Object> updatableValues = 
    correlator.extractUpdatableValues(customer, metamodel);

// Extract primary key value
Object pkValue = correlator.extractPrimaryKeyValue(customer, metamodel);
```

### 4. OperationOrchestrator

Orchestrates the complete workflow for each operation type.

```java
OperationOrchestrator orchestrator = new OperationOrchestrator(
    sqlBuilder, 
    executor, 
    converter
);

OperationResult<Customer> result = orchestrator.orchestrate(request);
```

## Operation Workflows

### INSERT Operation

```
1. Identify Operation
   └─> Type: INSERT

2. Correlate Entity with Metamodel
   ├─> Extract insertable field values
   ├─> Map Java fields → SQL columns
   └─> Exclude auto-increment primary key

3. Prepare SQL Statement
   ├─> SqlBuilder.buildInsert(metamodel)
   └─> SQL: INSERT INTO customers (name, email) VALUES (?, ?)

4. Execute
   ├─> SqlExecutor.executeInsert(statement, entity)
   ├─> Bind parameters from entity
   └─> Track execution time

5. Return Result
   ├─> Generated primary key
   ├─> Affected rows: 1
   └─> Execution plan with statistics
```

### SELECT_BY_ID Operation

```
1. Identify Operation
   └─> Type: SELECT_BY_ID

2. No Entity Correlation Needed
   └─> Only need primary key value

3. Prepare SQL Statement
   ├─> SqlBuilder.buildSelectById(metamodel)
   └─> SQL: SELECT * FROM customers WHERE id = ?

4. Execute
   ├─> SqlExecutor.executeQueryById(statement, id)
   └─> Get ResultSet

5. Convert Results
   ├─> EntityConverter.convertToEntity(rs, metamodel, class)
   ├─> Map SQL columns → Java fields
   └─> Return Customer object
```

### UPDATE Operation

```
1. Identify Operation
   └─> Type: UPDATE

2. Correlate Entity with Metamodel
   ├─> Extract updatable field values (exclude PK)
   ├─> Extract primary key value
   └─> Map fields → columns

3. Prepare SQL Statement
   ├─> SqlBuilder.buildUpdate(metamodel)
   └─> SQL: UPDATE customers SET name=?, email=? WHERE id=?

4. Execute
   ├─> SqlExecutor.executeUpdate(statement, entity)
   ├─> Bind updatable fields + PK
   └─> Track affected rows

5. Return Result
   └─> Affected rows count
```

### SELECT_WHERE Operation

```
1. Identify Operation
   └─> Type: SELECT_WHERE

2. No Entity Correlation Needed
   └─> WHERE clause provided in parameters

3. Prepare SQL Statement
   ├─> SqlBuilder.buildSelectWithWhere(metamodel, whereClause)
   └─> SQL: SELECT * FROM customers WHERE name LIKE ? AND age > ?

4. Execute
   ├─> SqlExecutor.executeQuery(statement)
   ├─> Bind WHERE parameters
   └─> Get ResultSet

5. Convert Results
   ├─> For each row: convertToEntity()
   └─> Return List<Customer>
```

## Usage Examples

### Basic INSERT

```java
// Old way (coupled)
EntityMetamodel metamodel = getMetamodel(entity.getClass());
SqlStatement statement = sqlBuilder.buildInsert(metamodel);
ExecutionResult result = executor.executeInsert(statement, entity);

// New way (orchestrated)
OperationRequest request = OperationRequest.builder()
    .operationType(OperationType.INSERT)
    .entity(customer)
    .metamodel(customerMetamodel)
    .build();

OperationResult<Customer> result = orchestrator.orchestrate(request);
int id = result.getGeneratedKey();
```

### Complex SELECT with WHERE

```java
WhereClause where = WhereClause.create()
    .like("name", "%John%")
    .greaterThan("age", 18);

OperationRequest request = OperationRequest.builder()
    .operationType(OperationType.SELECT_WHERE)
    .entityClass(Customer.class)
    .metamodel(customerMetamodel)
    .parameter("whereClause", where)
    .build();

OperationResult<Customer> result = orchestrator.orchestrate(request);
List<Customer> customers = result.getResultList();

// Access execution statistics
ExecutionPlan plan = result.getExecutionPlan();
System.out.println("Query executed in: " + plan.getExecutionTime() + "ms");
System.out.println("Rows returned: " + result.getAffectedRows());
```

### UPDATE with Validation

```java
customer.setEmail("new@email.com");

OperationRequest request = OperationRequest.builder()
    .operationType(OperationType.UPDATE)
    .entity(customer)
    .metamodel(customerMetamodel)
    .build();

OperationResult<Customer> result = orchestrator.orchestrate(request);

if (result.isSuccess()) {
    System.out.println("Updated " + result.getAffectedRows() + " row(s)");
} else {
    System.err.println("Update failed: " + result.getError().getMessage());
}
```

## Benefits

### 1. **Clear Separation of Concerns**

Each step has a dedicated component:
- **Identification**: Operation type from OperationRequest
- **Correlation**: EntityMetamodelCorrelator
- **SQL Building**: SqlBuilder
- **Execution**: SqlExecutor
- **Conversion**: EntityConverter

### 2. **Consistent Workflow**

All operations follow the same 5-step pattern, making the system predictable and maintainable.

### 3. **Better Testability**

```java
// Test correlation independently
@Test
public void testCorrelation() {
    EntityMetamodelCorrelator correlator = new EntityMetamodelCorrelator();
    Map<FieldMetamodel, Object> values = 
        correlator.extractInsertableValues(customer, metamodel);
    
    assertEquals("John", values.get(nameField));
}

// Test orchestration with mocks
@Test
public void testOrchestration() {
    SqlBuilder mockBuilder = mock(SqlBuilder.class);
    SqlExecutor mockExecutor = mock(SqlExecutor.class);
    
    OperationOrchestrator orchestrator = 
        new OperationOrchestrator(mockBuilder, mockExecutor, converter);
    
    // Test orchestration logic without database
}
```

### 4. **Execution Monitoring**

Every operation returns detailed execution statistics:

```java
OperationResult<Customer> result = orchestrator.orchestrate(request);
ExecutionPlan plan = result.getExecutionPlan();

System.out.println("SQL: " + plan.getSql());
System.out.println("Execution time: " + plan.getExecutionTime() + "ms");
System.out.println("Affected rows: " + plan.getAffectedRows());
System.out.println("Timestamp: " + plan.getTimestamp());
```

### 5. **Extensibility**

Easy to add new operation types or modify workflows:

```java
// Add BATCH_INSERT operation
case BATCH_INSERT:
    return orchestrateBatchInsert(request);

// Add caching layer before execution
@Override
public <T> OperationResult<T> orchestrate(OperationRequest request) {
    // Check cache first
    if (request.getOperationType() == SELECT_BY_ID) {
        T cached = cache.get(request.getPrimaryKeyValue());
        if (cached != null) {
            return OperationResult.<T>builder()
                .success(true)
                .result(cached)
                .build();
        }
    }
    
    // Execute operation
    return super.orchestrate(request);
}
```

## Comparison: Before vs After

| Aspect | Before | After |
|--------|--------|-------|
| **Responsibility** | RefactoredOrmMapper did everything | Orchestrator coordinates specialists |
| **Entity Correlation** | Mixed with SQL building | Dedicated EntityMetamodelCorrelator |
| **Operation Context** | Scattered parameters | Unified OperationRequest |
| **Result Handling** | Different types per operation | Unified OperationResult |
| **Testing** | Hard to isolate steps | Each step independently testable |
| **Workflow Clarity** | Implicit | Explicit 5-step pattern |
| **Extensibility** | Modify core methods | Add operation types |

## Summary

The orchestration layer provides:

✅ **Clear workflow**: 5 explicit steps for every operation  
✅ **Better separation**: Each component has single responsibility  
✅ **Entity correlation**: Dedicated component maps entities to metamodel  
✅ **Unified interface**: OperationRequest and OperationResult  
✅ **Execution tracking**: Detailed statistics for every operation  
✅ **Testability**: Components can be tested in isolation  
✅ **Extensibility**: Easy to add new operations or modify workflows  

The orchestrator is the brain that coordinates all ORM operations, ensuring consistent and maintainable execution patterns!

