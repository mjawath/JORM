# Advanced ORM Features Guide

## Overview

This guide covers the advanced features of the JSON-based ORM system:
1. **Query Builder** - Complex WHERE clauses with fluent API
2. **Relationship Mapping** - OneToMany, ManyToOne, OneToOne relationships
3. **Lazy Loading** - Load related entities on-demand
4. **Transaction Support** - ACID transactions with rollback
5. **Caching Layer** - LRU cache with TTL support

---

## 1. Query Builder for Complex WHERE Clauses

The QueryBuilder provides a fluent, type-safe API for constructing complex SQL queries without writing raw SQL.

### Basic Usage

```java
JsonOrmMapper mapper = new JsonOrmMapper("myapp.db");
mapper.loadMapping("classpath:orm/customer-mapping.json");

// Simple equality query
QueryBuilder<Customer> query = mapper.query(Customer.class)
    .eq("name", "John Doe");
List<Customer> results = mapper.executeQuery(query);
```

### Available Operations

#### Comparison Operators

```java
// Equals
.eq("field", value)

// Not equals
.ne("field", value)

// Greater than
.gt("field", value)

// Greater than or equal
.gte("field", value)

// Less than
.lt("field", value)

// Less than or equal
.lte("field", value)
```

#### Pattern Matching

```java
// LIKE query (use % for wildcards)
.like("email", "%@example.com")
```

#### Range Queries

```java
// BETWEEN
.between("price", 10.0, 100.0)

// IN
.in("status", Arrays.asList("active", "pending", "approved"))
```

#### NULL Checks

```java
// IS NULL
.isNull("deletedAt")

// IS NOT NULL
.isNotNull("email")
```

#### Logical Operators

```java
// AND is default - just chain conditions
query.eq("status", "active")
     .gt("price", 50.0);  // status = 'active' AND price > 50

// OR - call or() after a condition
query.eq("status", "active")
     .or()
     .eq("status", "pending");  // status = 'active' OR status = 'pending'
```

#### Ordering and Pagination

```java
// Order by field (ascending by default)
.orderBy("name")

// Order by field with direction
.orderBy("price", false)  // descending

// Limit results
.limit(10)

// Offset (for pagination)
.offset(20)
```

### Complex Query Examples

#### Example 1: Search with Multiple Conditions

```java
// Find active customers with email domain and recent activity
QueryBuilder<Customer> query = mapper.query(Customer.class)
    .eq("status", "active")
    .like("email", "%@company.com")
    .gte("lastLoginDate", "2025-01-01")
    .orderBy("name")
    .limit(50);

List<Customer> results = mapper.executeQuery(query);
```

#### Example 2: Price Range with Category Filter

```java
// Find products in price range within specific categories
QueryBuilder<Product> query = mapper.query(Product.class)
    .between("price", 100.0, 500.0)
    .in("category", Arrays.asList("Electronics", "Computers"))
    .orderBy("price", true)  // ascending
    .limit(20);

List<Product> products = mapper.executeQuery(query);
```

#### Example 3: Complex OR Conditions

```java
// Find high-value items OR items on sale
QueryBuilder<Sku> query = mapper.query(Sku.class)
    .gt("price", 1000.0)
    .or()
    .eq("onSale", true)
    .orderBy("price", false);  // descending

List<Sku> skus = mapper.executeQuery(query);
```

#### Example 4: Pagination

```java
int pageSize = 20;
int pageNumber = 2;  // zero-based

QueryBuilder<Customer> query = mapper.query(Customer.class)
    .orderBy("name")
    .limit(pageSize)
    .offset(pageNumber * pageSize);

List<Customer> page = mapper.executeQuery(query);
```

---

## 2. Transaction Support

Transactions ensure ACID properties for database operations. All operations in a transaction either succeed together or fail together.

### Using Transaction Callbacks

```java
EnhancedOrmMapper mapper = new EnhancedOrmMapper("myapp.db");
mapper.loadMapping("classpath:orm/customer-mapping.json");

// Automatic commit/rollback
mapper.executeInTransaction(m -> {
    Customer customer = new Customer();
    customer.setName("John Doe");
    int customerId = customerRepo.save(customer);
    
    // Multiple operations in same transaction
    Order order = new Order();
    order.setCustomerId(customerId);
    orderRepo.save(order);
    
    // If any exception occurs, all changes are rolled back
});
```

### Manual Transaction Control

```java
mapper.beginTransaction();
try {
    // Perform operations
    customerRepo.save(customer);
    orderRepo.save(order);
    
    // Commit if successful
    mapper.commit();
} catch (Exception e) {
    // Rollback on error
    mapper.rollback();
    throw e;
}
```

### Transaction Examples

#### Example 1: Creating Invoice with Line Items

```java
mapper.executeInTransaction(m -> {
    // Save invoice
    Invoice invoice = new Invoice();
    invoice.setNumber("INV-001");
    int invoiceId = invoiceRepo.save(invoice);
    
    // Save line items - if any fails, entire invoice is rolled back
    for (LineItem item : items) {
        item.setInvoiceId(invoiceId);
        lineItemRepo.save(item);
    }
});
```

#### Example 2: Transferring Inventory

```java
mapper.executeInTransaction(m -> {
    // Decrease stock at source
    Sku sku1 = skuRepo.findById(sourceId);
    sku1.setStock(sku1.getStock() - quantity);
    skuRepo.update(sku1);
    
    // Increase stock at destination
    Sku sku2 = skuRepo.findById(destId);
    sku2.setStock(sku2.getStock() + quantity);
    skuRepo.update(sku2);
    
    // Both updates happen atomically
});
```

---

## 3. Caching Layer

The caching layer improves performance by storing query results in memory with LRU eviction and TTL.

### Basic Usage

```java
EnhancedOrmMapper mapper = new EnhancedOrmMapper("myapp.db");
// Caching is automatic - queries are cached by SQL + parameters
```

### Cache Configuration

```java
// Create cache with custom settings
// OrmCache(maxSize, ttlMillis)
OrmCache cache = new OrmCache(200, 10 * 60 * 1000);  // 200 entries, 10 min TTL
```

### Cache Operations

```java
// Clear entire cache
mapper.clearCache();

// Evict specific entry
mapper.evictFromCache(cacheKey);

// Get cache statistics
OrmCache.CacheStats stats = cache.getStats();
System.out.println(stats);  // Cache[size=45/100, ttl=300000ms]
```

### How Caching Works

1. **First Query**: Hits database, result cached
2. **Subsequent Queries**: Returns cached result (much faster)
3. **Cache Expiry**: After TTL expires, next query hits database
4. **Cache Eviction**: When cache is full, least recently used entries are removed
5. **Cache Invalidation**: Cleared after transactions commit

### Performance Example

```java
// First query - database hit (~50ms)
QueryBuilder<Customer> q1 = mapper.query(Customer.class).eq("status", "active");
List<Customer> r1 = mapper.executeQuery(q1);  // 50ms

// Second query - cache hit (~1ms)
QueryBuilder<Customer> q2 = mapper.query(Customer.class).eq("status", "active");
List<Customer> r2 = mapper.executeQuery(q2);  // 1ms - 50x faster!
```

---

## 4. Relationship Mapping (OneToMany, ManyToOne)

Define relationships between entities using JSON configuration.

### Relationship Configuration

Add to your entity mapping JSON file:

```json
{
  "entityClass": "com.example.Customer",
  "tableName": "customer",
  "fields": [...],
  "relationships": {
    "orders": {
      "type": "ONE_TO_MANY",
      "targetEntity": "com.example.Order",
      "mappedBy": "customerId",
      "fetchType": "LAZY"
    }
  }
}
```

### Relationship Types

#### OneToMany

```json
{
  "orders": {
    "type": "ONE_TO_MANY",
    "targetEntity": "com.example.Order",
    "mappedBy": "customerId",
    "fetchType": "LAZY"
  }
}
```

#### ManyToOne

```json
{
  "customer": {
    "type": "MANY_TO_ONE",
    "targetEntity": "com.example.Customer",
    "joinColumn": "customer_id",
    "fetchType": "EAGER"
  }
}
```

#### OneToOne

```json
{
  "profile": {
    "type": "ONE_TO_ONE",
    "targetEntity": "com.example.UserProfile",
    "joinColumn": "profile_id",
    "fetchType": "LAZY"
  }
}
```

### Fetch Types

- **LAZY**: Load relationship only when accessed (better performance)
- **EAGER**: Load relationship immediately with parent entity

---

## 5. Lazy Loading

Lazy loading defers loading of related entities until they're actually accessed.

### Configuration

Set `fetchType` to `LAZY` in relationship mapping:

```json
{
  "orders": {
    "type": "ONE_TO_MANY",
    "targetEntity": "com.example.Order",
    "fetchType": "LAZY"
  }
}
```

### How It Works

```java
// Load customer (orders not loaded yet)
Customer customer = customerRepo.findById(1);

// Orders are loaded only when accessed
List<Order> orders = customer.getOrders();  // Database query happens here
```

### Benefits

1. **Better Performance**: Don't load data you won't use
2. **Reduced Memory**: Only loaded entities consume memory
3. **Network Efficiency**: Fewer database round-trips

### When to Use

- **Use LAZY** for large collections or rarely accessed relationships
- **Use EAGER** for small, frequently accessed relationships

---

## Complete Example: E-Commerce System

```java
// Setup
EnhancedOrmMapper mapper = new EnhancedOrmMapper("ecommerce.db");
mapper.loadMapping("classpath:orm/customer-mapping.json");
mapper.loadMapping("classpath:orm/order-mapping.json");
mapper.loadMapping("classpath:orm/product-mapping.json");

GenericRepository<Customer> customerRepo = new GenericRepository<>(mapper, Customer.class);
GenericRepository<Order> orderRepo = new GenericRepository<>(mapper, Order.class);

// 1. Query Builder: Find high-value customers
QueryBuilder<Customer> query = mapper.query(Customer.class)
    .eq("status", "premium")
    .gte("totalSpent", 1000.0)
    .orderBy("totalSpent", false)
    .limit(10);
List<Customer> premiumCustomers = mapper.executeQuery(query);

// 2. Transaction: Create order with items
mapper.executeInTransaction(m -> {
    Order order = new Order();
    order.setCustomerId(customer.getId());
    int orderId = orderRepo.save(order);
    
    for (OrderItem item : items) {
        item.setOrderId(orderId);
        orderItemRepo.save(item);
    }
});

// 3. Caching: Repeated queries are fast
QueryBuilder<Product> productQuery = mapper.query(Product.class)
    .eq("category", "Electronics");
List<Product> products1 = mapper.executeQuery(productQuery);  // DB hit
List<Product> products2 = mapper.executeQuery(productQuery);  // Cache hit - fast!

// 4. Lazy Loading: Load orders only when needed
Customer customer = customerRepo.findById(customerId);
// Orders not loaded yet - efficient if we don't need them
if (needOrderDetails) {
    List<Order> orders = customer.getOrders();  // Loaded on demand
}
```

---

## Performance Tips

1. **Use Query Builder** instead of loading all records and filtering in Java
2. **Enable Caching** for frequently executed queries
3. **Use Transactions** to batch multiple operations
4. **Use Lazy Loading** for relationships you might not need
5. **Add Indexes** to columns used in WHERE clauses (manual SQL)
6. **Limit Results** when you don't need all records

---

## Testing

Run the comprehensive demo:

```bash
mvn compile exec:java -Dexec.mainClass="com.pos.service.orm.AdvancedOrmDemo"
```

This will demonstrate all advanced features with real examples and performance metrics.

