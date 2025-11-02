# EntityManager Quick Reference

## Setup

```java
// Create factory
EntityManagerFactory emf = EntityManagerFactoryImpl.create("database.db");

// Register entities
emf.registerMappings(
    "classpath:orm/customer-mapping.json",
    "classpath:orm/sku-mapping.json"
);

// Create EntityManager
EntityManager em = emf.createEntityManager();
```

## CRUD Operations

```java
// CREATE
Object id = em.persist(customer);

// READ
Customer customer = em.find(Customer.class, id);
Optional<Customer> optional = em.findOptional(Customer.class, id);
List<Customer> all = em.findAll(Customer.class);

// UPDATE
customer.setEmail("new@email.com");
em.merge(customer);

// DELETE
em.remove(customer);
em.remove(Customer.class, id);
```

## Transactions

```java
EntityTransaction tx = em.getTransaction();

tx.begin();
try {
    em.persist(entity1);
    em.merge(entity2);
    tx.commit();
} catch (Exception e) {
    tx.rollback();
}
```

## QueryBuilder

```java
// Simple query
List<Customer> results = em.createQueryBuilder(Customer.class)
    .where(whereClause)
    .getResultList();

// With pagination
List<Customer> page = em.createQueryBuilder(Customer.class)
    .setFirstResult(0)
    .setMaxResults(10)
    .getResultList();

// With ordering
List<Customer> ordered = em.createQueryBuilder(Customer.class)
    .orderByDesc("created_date")
    .getResultList();

// Single result
Customer customer = em.createQueryBuilder(Customer.class)
    .where(whereClause)
    .getSingleResult();

// Count
long count = em.createQueryBuilder(Customer.class)
    .where(whereClause)
    .count();
```

## CriteriaQuery

```java
// Type-safe query
List<Sku> skus = em.createCriteria(Sku.class)
    .greaterThan("price", 20.0)
    .lessThan("stock_quantity", 100)
    .like("description", "%Product%")
    .getResultList();

// With IN clause
List<Sku> matched = em.createCriteria(Sku.class)
    .in("code", Arrays.asList("SKU001", "SKU002"))
    .getResultList();

// Count
long count = em.createCriteria(Sku.class)
    .equal("status", "active")
    .count();
```

## Utility Methods

```java
// Count
long total = em.count(Customer.class);
long active = em.count(Customer.class, whereClause);

// Exists
boolean exists = em.exists(Customer.class, id);

// Refresh
em.refresh(customer);  // Reload from DB

// Contains (managed?)
boolean managed = em.contains(customer);

// Clear persistence context
em.clear();

// Batch delete
int deleted = em.deleteWhere(Customer.class, whereClause);
```

## Cleanup

```java
// Close EntityManager
em.close();

// Close factory (at application shutdown)
emf.close();
```

## Common Patterns

### Pattern 1: Simple Operation
```java
EntityManager em = emf.createEntityManager();
try {
    em.persist(customer);
} finally {
    em.close();
}
```

### Pattern 2: Transaction
```java
EntityManager em = emf.createEntityManager();
EntityTransaction tx = em.getTransaction();
try {
    tx.begin();
    em.persist(customer);
    tx.commit();
} catch (Exception e) {
    if (tx.isActive()) tx.rollback();
} finally {
    em.close();
}
```

### Pattern 3: Query
```java
EntityManager em = emf.createEntityManager();
try {
    List<Customer> customers = em.createQueryBuilder(Customer.class)
        .where(whereClause)
        .getResultList();
    // Process results
} finally {
    em.close();
}
```

## Tips

✅ Always close EntityManager after use  
✅ Use transactions for multiple operations  
✅ Use Optional for nullable results  
✅ Use QueryBuilder for complex queries  
✅ Use CriteriaQuery for type-safety  
✅ Register mappings at startup  
✅ Reuse EntityManagerFactory  
✅ Create new EntityManager per operation  

## Comparison: Old vs New

```java
// OLD: RefactoredOrmMapper
RefactoredOrmMapper orm = new RefactoredOrmMapper("db.db");
orm.loadMapping("config.json");
int id = orm.insert(customer);
Customer found = orm.findById(Customer.class, id);
orm.update(found);

// NEW: EntityManager (JPA-like)
EntityManagerFactory emf = EntityManagerFactoryImpl.create("db.db");
emf.registerMapping("config.json");
EntityManager em = emf.createEntityManager();
Object id = em.persist(customer);
Customer found = em.find(Customer.class, id);
em.merge(found);
em.close();
```

Both work! EntityManager is recommended for new code.

