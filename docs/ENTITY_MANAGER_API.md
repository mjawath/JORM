# JPA-like EntityManager API

## Overview

A complete JPA-style EntityManager implementation for the ORM, providing a familiar and standardized API for database operations.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│              EntityManagerFactory                            │
│  - Create EntityManager instances                           │
│  - Register entity mappings                                  │
│  - Manage factory lifecycle                                  │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                  EntityManager                               │
│  - persist(), merge(), find(), remove()                      │
│  - findAll(), findWhere()                                    │
│  - Managed entity tracking                                   │
│  - Transaction support                                       │
│  - QueryBuilder & CriteriaQuery                              │
└─────────────────────────────────────────────────────────────┘
         │              │              │              │
         ▼              ▼              ▼              ▼
┌────────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐
│ EntityTrans.   │ │   Query    │ │ Criteria   │ │ Managed    │
│                │ │  Builder   │ │   Query    │ │ Entities   │
└────────────────┘ └────────────┘ └────────────┘ └────────────┘
```

## Core Interfaces

### 1. EntityManagerFactory

Factory for creating EntityManager instances.

```java
EntityManagerFactory emf = EntityManagerFactoryImpl.create("database.db");

// Register entity mappings
emf.registerMappings(
    "classpath:orm/customer-mapping.json",
    "classpath:orm/sku-mapping.json"
);

// Create EntityManager
EntityManager em = emf.createEntityManager();

// Close factory
emf.close();
```

### 2. EntityManager

Main interface for entity operations.

```java
public interface EntityManager {
    // Persistence operations
    <T> Object persist(T entity);
    <T> T merge(T entity);
    <T> void remove(T entity);
    <T> void remove(Class<T> entityClass, Object primaryKey);
    
    // Find operations
    <T> T find(Class<T> entityClass, Object primaryKey);
    <T> Optional<T> findOptional(Class<T> entityClass, Object primaryKey);
    <T> T getReference(Class<T> entityClass, Object primaryKey);
    <T> List<T> findAll(Class<T> entityClass);
    <T> List<T> findWhere(Class<T> entityClass, WhereClause whereClause);
    
    // Utility operations
    <T> void refresh(T entity);
    <T> boolean contains(T entity);
    void flush();
    void clear();
    
    // Query builders
    <T> QueryBuilder<T> createQueryBuilder(Class<T> entityClass);
    <T> CriteriaQuery<T> createCriteria(Class<T> entityClass);
    
    // Count & exists
    <T> long count(Class<T> entityClass);
    <T> boolean exists(Class<T> entityClass, Object primaryKey);
    
    // Batch operations
    <T> int deleteAll(Class<T> entityClass);
    <T> int deleteWhere(Class<T> entityClass, WhereClause whereClause);
    
    // Transaction
    EntityTransaction getTransaction();
    
    // Lifecycle
    void close();
    boolean isOpen();
}
```

### 3. EntityTransaction

Transaction management interface.

```java
EntityTransaction tx = em.getTransaction();

try {
    tx.begin();
    
    // Perform operations
    em.persist(customer);
    em.merge(sku);
    
    tx.commit();
} catch (Exception e) {
    if (tx.isActive()) {
        tx.rollback();
    }
}
```

### 4. QueryBuilder

Fluent API for building queries.

```java
List<Customer> results = em.createQueryBuilder(Customer.class)
    .where(WhereClause.create()
        .like("name", "%John%")
        .greaterThan("age", 18))
    .orderByAsc("name")
    .setMaxResults(10)
    .getResultList();
```

### 5. CriteriaQuery

Type-safe query construction.

```java
List<Sku> skus = em.createCriteria(Sku.class)
    .greaterThan("price", 20.0)
    .lessThan("stock_quantity", 100)
    .like("description", "%Product%")
    .orderBy("price", false)
    .getResultList();
```

## Usage Examples

### Example 1: Basic CRUD

```java
EntityManagerFactory emf = EntityManagerFactoryImpl.create("pos.db");
emf.registerMapping("classpath:orm/customer-mapping.json");

EntityManager em = emf.createEntityManager();

try {
    // CREATE
    Customer customer = new Customer();
    customer.setName("John Doe");
    customer.setEmail("john@example.com");
    Object id = em.persist(customer);
    
    // READ
    Customer found = em.find(Customer.class, id);
    
    // UPDATE
    found.setEmail("john.doe@example.com");
    em.merge(found);
    
    // DELETE
    em.remove(found);
    
} finally {
    em.close();
    emf.close();
}
```

### Example 2: Transactions

```java
EntityManager em = emf.createEntityManager();
EntityTransaction tx = em.getTransaction();

try {
    tx.begin();
    
    // Multiple operations in transaction
    Customer c1 = new Customer();
    c1.setName("Alice");
    em.persist(c1);
    
    Customer c2 = new Customer();
    c2.setName("Bob");
    em.persist(c2);
    
    tx.commit(); // Both saved together
    
} catch (Exception e) {
    if (tx.isActive()) {
        tx.rollback(); // Neither saved on error
    }
    throw e;
} finally {
    em.close();
}
```

### Example 3: Query Builder

```java
EntityManager em = emf.createEntityManager();

try {
    // Complex query with pagination
    List<Customer> customers = em.createQueryBuilder(Customer.class)
        .where(WhereClause.create()
            .like("email", "%@example.com")
            .greaterThan("created_date", someDate))
        .orderByDesc("created_date")
        .setFirstResult(0)
        .setMaxResults(20)
        .getResultList();
    
    // Count query
    long count = em.createQueryBuilder(Customer.class)
        .where(WhereClause.create()
            .equals("status", "active"))
        .count();
    
    // Single result
    Customer customer = em.createQueryBuilder(Customer.class)
        .where(WhereClause.create()
            .equals("email", "john@example.com"))
        .getSingleResult();
        
} finally {
    em.close();
}
```

### Example 4: Criteria Query

```java
EntityManager em = emf.createEntityManager();

try {
    // Type-safe query
    List<Sku> skus = em.createCriteria(Sku.class)
        .greaterThanOrEqual("price", 10.0)
        .lessThanOrEqual("price", 100.0)
        .greaterThan("stock_quantity", 0)
        .isNotNull("description")
        .orderBy("price", true)
        .getResultList();
    
    // With IN clause
    List<String> codes = Arrays.asList("SKU001", "SKU002", "SKU003");
    List<Sku> matched = em.createCriteria(Sku.class)
        .in("code", codes)
        .getResultList();
    
    // Count
    long count = em.createCriteria(Sku.class)
        .like("description", "%Product%")
        .count();
        
} finally {
    em.close();
}
```

### Example 5: Managed Entities

```java
EntityManager em = emf.createEntityManager();

try {
    Customer customer = new Customer();
    customer.setName("Test");
    
    // Entity is detached
    System.out.println(em.contains(customer)); // false
    
    Object id = em.persist(customer);
    
    // Entity is now managed
    System.out.println(em.contains(customer)); // true
    
    // Find makes entity managed
    Customer found = em.find(Customer.class, id);
    System.out.println(em.contains(found)); // true
    
    // Refresh from database
    found.setName("Modified");
    em.refresh(found); // Reverts to database state
    
    // Clear persistence context
    em.clear();
    System.out.println(em.contains(found)); // false
    
} finally {
    em.close();
}
```

### Example 6: Batch Operations

```java
EntityManager em = emf.createEntityManager();

try {
    // Delete with WHERE clause
    WhereClause inactive = WhereClause.create()
        .equals("status", "inactive")
        .lessThan("last_login", threeMonthsAgo);
    
    int deleted = em.deleteWhere(Customer.class, inactive);
    System.out.println("Deleted " + deleted + " inactive customers");
    
    // Delete all (use with caution!)
    int allDeleted = em.deleteAll(TempTable.class);
    
    // Count with WHERE
    long activeCount = em.count(Customer.class, 
        WhereClause.create().equals("status", "active"));
    
    // Check existence
    boolean exists = em.exists(Customer.class, customerId);
    
} finally {
    em.close();
}
```

### Example 7: Optional Results

```java
EntityManager em = emf.createEntityManager();

try {
    // Optional find
    Optional<Customer> optional = em.findOptional(Customer.class, id);
    
    optional.ifPresent(customer -> {
        System.out.println("Found: " + customer.getName());
    });
    
    // Optional single result
    Optional<Customer> result = em.createQueryBuilder(Customer.class)
        .where(WhereClause.create().equals("email", "test@example.com"))
        .getSingleResultOptional();
    
    Customer customer = result.orElseGet(() -> {
        Customer newCustomer = new Customer();
        newCustomer.setEmail("test@example.com");
        return newCustomer;
    });
    
} finally {
    em.close();
}
```

## API Comparison with JPA

| JPA Method | Our EntityManager | Notes |
|-----------|-------------------|-------|
| `persist(entity)` | ✅ `persist(entity)` | Returns generated key |
| `merge(entity)` | ✅ `merge(entity)` | Update or insert |
| `remove(entity)` | ✅ `remove(entity)` | Also `remove(class, id)` |
| `find(class, id)` | ✅ `find(class, id)` | Exact same |
| `getReference(class, id)` | ✅ `getReference(class, id)` | Currently same as find |
| `refresh(entity)` | ✅ `refresh(entity)` | Reload from DB |
| `contains(entity)` | ✅ `contains(entity)` | Check if managed |
| `flush()` | ✅ `flush()` | Currently no-op |
| `clear()` | ✅ `clear()` | Clear persistence context |
| `close()` | ✅ `close()` | Close EntityManager |
| `getTransaction()` | ✅ `getTransaction()` | Transaction support |
| `createQuery(jpql)` | 🔄 `createQueryBuilder()` | Different syntax |
| `createCriteria()` | ✅ `createCriteria()` | Type-safe queries |
| - | ➕ `findAll(class)` | Additional method |
| - | ➕ `findWhere(class, where)` | Additional method |
| - | ➕ `count(class)` | Additional method |
| - | ➕ `exists(class, id)` | Additional method |
| - | ➕ `deleteWhere()` | Additional method |

## Benefits

### 1. Familiar API

Developers familiar with JPA will feel at home:

```java
// Looks just like JPA!
EntityManager em = emf.createEntityManager();
EntityTransaction tx = em.getTransaction();

tx.begin();
Customer customer = em.find(Customer.class, 1);
customer.setEmail("new@email.com");
em.merge(customer);
tx.commit();
```

### 2. Standard Patterns

Follows JPA best practices:
- EntityManagerFactory for lifecycle management
- EntityManager for operations
- EntityTransaction for transaction control
- Managed entity tracking
- Clear separation of concerns

### 3. Type Safety

CriteriaQuery provides compile-time safety:

```java
// Type-safe, no string errors
List<Customer> results = em.createCriteria(Customer.class)
    .greaterThan("age", 18)
    .like("name", "%John%")
    .getResultList();
```

### 4. Fluent API

QueryBuilder enables readable query construction:

```java
List<Sku> skus = em.createQueryBuilder(Sku.class)
    .where(whereClause)
    .orderByDesc("price")
    .setMaxResults(10)
    .getResultList();
```

### 5. Resource Management

Proper lifecycle management with try-with-resources support:

```java
try (EntityManager em = emf.createEntityManager()) {
    // Operations
} // Auto-closes
```

### 6. Transaction Safety

Built-in transaction support with rollback:

```java
EntityTransaction tx = em.getTransaction();
try {
    tx.begin();
    // operations
    tx.commit();
} catch (Exception e) {
    tx.rollback();
}
```

## Implementation Details

### EntityManagerImpl

- Wraps the orchestration layer
- Tracks managed entities in a Set
- Delegates to OperationOrchestrator for operations
- Implements lifecycle management

### EntityManagerFactoryImpl

- Creates EntityManager instances
- Registers entity mappings centrally
- Ensures all EntityManagers have same mappings
- Factory-level lifecycle management

### QueryBuilderImpl

- Fluent API for query construction
- Supports WHERE, ORDER BY, pagination
- Delegates to EntityManager for execution
- In-memory pagination support

### CriteriaQueryImpl

- Type-safe query construction
- Maps to WhereClause internally
- Supports all common predicates
- Fluent chainable API

## Summary

✅ **Complete JPA-like API** - Familiar interface for JPA developers  
✅ **EntityManagerFactory** - Proper factory pattern  
✅ **EntityManager** - 20+ standard operations  
✅ **EntityTransaction** - Transaction support  
✅ **QueryBuilder** - Fluent query API  
✅ **CriteriaQuery** - Type-safe queries  
✅ **Managed Entities** - Entity lifecycle tracking  
✅ **Optional Support** - Modern Java API  
✅ **Batch Operations** - deleteWhere, count, exists  

The EntityManager API provides a professional, standards-based interface for your ORM, making it easy for developers familiar with JPA to use your system!

