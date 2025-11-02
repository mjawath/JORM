# One-to-Many Relationship Guide: Invoice & Line Items

## Overview

This guide demonstrates how to handle **one-to-many relationships** in the ORM system, using the classic example of a **Sales Invoice having multiple Line Items**.

## Architecture

```
Invoice (One)
    ├── id (PK)
    ├── number
    ├── date
    ├── customerId (FK)
    ├── total
    └── lineItems (List) ───┐
                             │
                             ↓
                    LineItem (Many)
                        ├── id (PK)
                        ├── invoiceId (FK) ←─ points back to Invoice
                        ├── skuId (FK)
                        ├── quantity
                        ├── price
                        └── lineAmount
```

## Key Components

### 1. Model Classes

**Invoice.java** - Parent entity
```java
public class Invoice {
    private Integer id;
    private String number;
    private String date;
    private Integer customerId;
    private BigDecimal total;
    
    // OneToMany relationship
    private List<LineItem> lineItems = new ArrayList<>();
    
    // Helper method to maintain relationship
    public void addLineItem(LineItem item) {
        this.lineItems.add(item);
        item.setInvoiceId(this.id);
    }
}
```

**LineItem.java** - Child entity
```java
public class LineItem {
    private Integer id;
    private Integer invoiceId;  // Foreign key to Invoice
    private String skuName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal lineAmount;
    
    // ManyToOne relationship
    private Invoice invoice;
}
```

### 2. JSON Mapping Configuration

**invoice-mapping.json**
```json
{
  "entityClass": "com.mycompany.posswing.model.Invoice",
  "tableName": "invoice",
  "primaryKey": "id",
  "fields": [...],
  "relationships": {
    "lineItems": {
      "type": "ONE_TO_MANY",
      "targetEntity": "com.mycompany.posswing.model.LineItem",
      "mappedBy": "invoiceId",
      "fetchType": "LAZY"
    }
  }
}
```

**lineitem-mapping.json**
```json
{
  "entityClass": "com.mycompany.posswing.model.LineItem",
  "tableName": "line_item",
  "primaryKey": "id",
  "fields": [
    {
      "javaField": "invoiceId",
      "columnName": "invoice_id",
      "columnType": "INTEGER",
      "foreignKey": {
        "referencedTable": "invoice",
        "referencedColumn": "id"
      }
    },
    ...
  ]
}
```

### 3. Repository Pattern

**InvoiceRepository.java** provides high-level operations:

```java
public class InvoiceRepository {
    // Save invoice with all line items atomically
    public Invoice saveInvoiceWithLineItems(Invoice invoice);
    
    // Load invoice with all line items
    public Invoice findInvoiceWithLineItems(Integer invoiceId);
    
    // Update invoice and line items
    public void updateInvoiceWithLineItems(Invoice invoice);
    
    // Delete invoice and all line items
    public void deleteInvoiceWithLineItems(Integer invoiceId);
}
```

## Usage Examples

### Example 1: Creating an Invoice with Line Items

```java
InvoiceRepository repo = new InvoiceRepository("myapp.db");

// Create invoice
Invoice invoice = new Invoice();
invoice.setNumber("INV-001");
invoice.setDate("2025-01-26");
invoice.setCustomerId(customerId);

// Add line items
LineItem item1 = new LineItem();
item1.setSkuName("Laptop");
item1.setQuantity(2);
item1.setPrice(BigDecimal.valueOf(999.99));
item1.setLineAmount(BigDecimal.valueOf(1999.98));
invoice.addLineItem(item1);

LineItem item2 = new LineItem();
item2.setSkuName("Mouse");
item2.setQuantity(2);
item2.setPrice(BigDecimal.valueOf(29.99));
item2.setLineAmount(BigDecimal.valueOf(59.98));
invoice.addLineItem(item2);

// Calculate totals
BigDecimal total = BigDecimal.valueOf(2059.96);
invoice.setTotal(total);
invoice.setTax(total.multiply(BigDecimal.valueOf(0.1)));
invoice.setPayment(total.add(invoice.getTax()));

// Save everything in one transaction
invoice = repo.saveInvoiceWithLineItems(invoice);

System.out.println("Created Invoice: " + invoice.getNumber());
System.out.println("Line Items: " + invoice.getLineItems().size());
```

**Output:**
```
Created Invoice: INV-001
Line Items: 2
```

### Example 2: Retrieving an Invoice with Line Items

```java
// Load invoice with all its line items
Invoice invoice = repo.findInvoiceWithLineItems(invoiceId);

System.out.println("Invoice: " + invoice.getNumber());
System.out.println("Date: " + invoice.getDate());
System.out.println("Total: $" + invoice.getTotal());
System.out.println("\nLine Items:");

for (LineItem item : invoice.getLineItems()) {
    System.out.printf("  • %s - Qty: %d × $%.2f = $%.2f%n",
        item.getSkuName(),
        item.getQuantity(),
        item.getPrice(),
        item.getLineAmount());
}
```

**Output:**
```
Invoice: INV-001
Date: 2025-01-26
Total: $2059.96

Line Items:
  • Laptop - Qty: 2 × $999.99 = $1999.98
  • Mouse - Qty: 2 × $29.99 = $59.98
```

### Example 3: Updating an Invoice (Adding More Items)

```java
// Load existing invoice
Invoice invoice = repo.findInvoiceWithLineItems(invoiceId);

// Add new line item
LineItem newItem = new LineItem();
newItem.setSkuName("Keyboard");
newItem.setQuantity(2);
newItem.setPrice(BigDecimal.valueOf(79.99));
newItem.setLineAmount(BigDecimal.valueOf(159.98));
invoice.addLineItem(newItem);

// Recalculate totals
BigDecimal total = invoice.getLineItems().stream()
    .map(LineItem::getLineAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);
invoice.setTotal(total);
invoice.setTax(total.multiply(BigDecimal.valueOf(0.1)));
invoice.setPayment(total.add(invoice.getTax()));

// Update in database
repo.updateInvoiceWithLineItems(invoice);

System.out.println("Updated invoice with " + invoice.getLineItems().size() + " items");
```

### Example 4: Query Invoices by Customer

```java
// Find all invoices for a customer (with line items loaded)
List<Invoice> invoices = repo.findInvoicesByCustomer(customerId);

System.out.println("Found " + invoices.size() + " invoices:");
for (Invoice inv : invoices) {
    System.out.println("  - " + inv.getNumber() + 
        " | Date: " + inv.getDate() +
        " | Items: " + inv.getLineItems().size() +
        " | Total: $" + inv.getTotal());
}
```

**Output:**
```
Found 3 invoices:
  - INV-001 | Date: 2025-01-26 | Items: 3 | Total: $2219.94
  - INV-002 | Date: 2025-01-25 | Items: 2 | Total: $1050.00
  - INV-003 | Date: 2025-01-24 | Items: 5 | Total: $3499.99
```

### Example 5: Deleting an Invoice with Line Items

```java
// Delete invoice and all its line items atomically
repo.deleteInvoiceWithLineItems(invoiceId);

System.out.println("Deleted invoice and all line items");
```

## How It Works

### 1. **Transaction Management**

All operations that involve both Invoice and LineItems use transactions to ensure data consistency:

```java
mapper.executeInTransaction(m -> {
    // Save invoice
    int invoiceId = invoiceRepo.save(invoice);
    
    // Save all line items with the invoice ID
    for (LineItem item : invoice.getLineItems()) {
        item.setInvoiceId(invoiceId);
        lineItemRepo.save(item);
    }
    
    // If any step fails, everything is rolled back
});
```

### 2. **Lazy Loading**

Line items are configured with `LAZY` fetch type, meaning they're only loaded when explicitly requested:

```java
// This only loads the invoice
Invoice invoice = invoiceRepo.findById(invoiceId);

// This loads the line items
invoice = repo.findInvoiceWithLineItems(invoiceId);
```

### 3. **Query Builder for Related Entities**

Use the query builder to find line items by invoice:

```java
QueryBuilder<LineItem> query = mapper.query(LineItem.class)
    .eq("invoiceId", invoiceId)
    .orderBy("id");
    
List<LineItem> items = mapper.executeQuery(query);
```

### 4. **Cascading Operations**

When deleting an invoice, all line items must be deleted first (foreign key constraint):

```java
// 1. Delete line items first
deleteLineItemsByInvoice(invoiceId);

// 2. Then delete invoice
invoiceRepo.delete(invoiceId);
```

## Best Practices

### 1. **Always Use Transactions**
```java
// ✓ Good: Atomic operation
repo.saveInvoiceWithLineItems(invoice);

// ✗ Bad: Can leave partial data
invoiceRepo.save(invoice);
for (LineItem item : items) {
    lineItemRepo.save(item); // What if this fails midway?
}
```

### 2. **Load Related Entities When Needed**
```java
// ✓ Good: Only load when needed
Invoice invoice = repo.findInvoiceWithLineItems(id);

// ✗ Bad: Loading all invoices with line items (slow)
List<Invoice> all = invoiceRepo.findAll();
for (Invoice inv : all) {
    inv.setLineItems(repo.findLineItemsByInvoice(inv.getId()));
}
```

### 3. **Maintain Bidirectional Relationships**
```java
// Use helper method to keep both sides in sync
public void addLineItem(LineItem item) {
    this.lineItems.add(item);
    item.setInvoiceId(this.id);  // Keep FK in sync
}
```

### 4. **Recalculate Aggregates**
```java
// Always recalculate totals when line items change
BigDecimal total = invoice.getLineItems().stream()
    .map(LineItem::getLineAmount)
    .reduce(BigDecimal.ZERO, BigDecimal::add);
invoice.setTotal(total);
```

## Running the Demo

Execute the comprehensive demo:

```bash
mvn compile exec:java -Dexec.mainClass="com.pos.service.orm.OneToManyRelationshipDemo"
```

This will demonstrate:
- Creating invoices with multiple line items
- Retrieving invoices with line items loaded
- Updating invoices (adding/removing items)
- Querying invoices by customer
- Querying invoices by date range
- Deleting invoices with all line items

## Summary

✓ **Model Classes**: Invoice (parent) and LineItem (child) with List relationship  
✓ **JSON Configuration**: Define ONE_TO_MANY and MANY_TO_ONE relationships  
✓ **Repository Pattern**: High-level operations that handle both entities  
✓ **Transactions**: Ensure atomic operations across related entities  
✓ **Query Builder**: Find related entities with type-safe queries  
✓ **Lazy Loading**: Load relationships only when needed for performance  

This pattern works for any one-to-many relationship:
- **Customer → Orders**
- **Order → OrderItems**
- **Product → Reviews**
- **Category → Products**
- **Department → Employees**

