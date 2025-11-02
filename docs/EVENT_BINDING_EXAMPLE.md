# Event Binding System - Usage Guide

## Overview
The `UILayoutRenderer` now supports flexible event binding through the `SalesScreenEventBus` and `ComponentEventBinding` interface. This allows you to wire UI components to business logic in a clean, generic, and maintainable way.

## Key Components

### 1. SalesScreenEventBus
A centralized event bus that manages event handlers by name.

### 2. UILayoutRenderer
The generic renderer that creates Swing components from a UILayout and wires events.

### 3. ComponentEventBinding
A functional interface for defining custom event wiring logic.

## Basic Usage

```java
// 1. Create the event bus
SalesScreenEventBus eventBus = new SalesScreenEventBus();

// 2. Setup data sources for comboboxes
Map<String, List<?>> dataSources = new HashMap<>();
dataSources.put("sku", skuList);  // Component ID -> List of entities
dataSources.put("customer", customerList);

// 3. Setup display functions for comboboxes
Map<String, Function<Object, String>> displayFunctions = new HashMap<>();
displayFunctions.put("sku", obj -> obj instanceof Sku ? ((Sku) obj).getName() : "");
displayFunctions.put("customer", obj -> obj instanceof Customer ? ((Customer) obj).getName() : "");

// 4. Setup custom event bindings (optional)
Map<String, UILayoutRenderer.ComponentEventBinding> eventBindings = new HashMap<>();

// Custom binding for quantity field
eventBindings.put("quantity", (component, context, bus) -> {
    if (component instanceof JTextField) {
        JTextField tf = (JTextField) component;
        tf.addActionListener(e -> {
            try {
                int quantity = Integer.parseInt(tf.getText());
                Object selectedSku = context.get("selected_sku");
                
                if (selectedSku == null) {
                    JOptionPane.showMessageDialog(tf, 
                        "Please select a SKU first", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                bus.fire("quantityEntered", selectedSku, quantity, context);
                tf.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(tf, 
                    "Invalid quantity", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
});

// 5. Register business logic event handlers
eventBus.register("quantityEntered", args -> {
    Object sku = args[0];
    int quantity = (int) args[1];
    Map<String, Object> context = (Map<String, Object>) args[2];
    
    // Find table model from context
    DefaultTableModel tableModel = null;
    for (Map.Entry<String, Object> entry : context.entrySet()) {
        if (entry.getKey().startsWith("tableModel_")) {
            tableModel = (DefaultTableModel) entry.getValue();
            break;
        }
    }
    
    if (tableModel != null && sku instanceof Sku) {
        Sku skuObj = (Sku) sku;
        Object[] row = {skuObj.getCode(), skuObj.getName(), quantity};
        tableModel.addRow(row);
    }
});

eventBus.register("skuSelected", args -> {
    Object selected = args[0];
    System.out.println("SKU selected: " + selected);
});

// 6. Create renderer and render the layout
UILayoutRenderer renderer = new UILayoutRenderer(
    eventBus, dataSources, displayFunctions, eventBindings
);
JPanel panel = renderer.render(layout);
```

## Default Event Behavior

If no custom event binding is provided for a component, the renderer automatically wires:

### JComboBox
- Fires: `{componentId}Selected`
- Args: `[selectedItem, context]`
- Updates context: `selected_{componentId}` = selectedItem

### JTextField
- Fires: `{componentId}Entered`
- Args: `[textValue, context, textField]`
- Triggered: When user presses Enter

### JButton
- Fires: `{componentId}Clicked`
- Args: `[context, button]`
- Triggered: When button is clicked

## JSON Layout Example

```json
{
  "components": [
    {
      "id": "sku",
      "type": "combobox",
      "x": 10,
      "y": 10,
      "width": 200,
      "height": 30
    },
    {
      "id": "quantity",
      "type": "textfield",
      "placeholder": "Enter quantity",
      "x": 220,
      "y": 10,
      "width": 100,
      "height": 30
    },
    {
      "id": "salesTable",
      "type": "table",
      "columns": ["SKU", "Name", "Quantity"],
      "x": 10,
      "y": 50,
      "width": 760,
      "height": 400
    },
    {
      "id": "checkout",
      "type": "button",
      "text": "Checkout",
      "x": 670,
      "y": 460,
      "width": 100,
      "height": 30
    }
  ]
}
```

## Advanced: Custom Event Binding

You can create complex event bindings that interact with multiple components:

```java
eventBindings.put("search", (component, context, bus) -> {
    if (component instanceof JTextField) {
        JTextField searchField = (JTextField) component;
        
        // Real-time search as user types
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { search(); }
            public void removeUpdate(DocumentEvent e) { search(); }
            public void changedUpdate(DocumentEvent e) { search(); }
            
            private void search() {
                String query = searchField.getText();
                bus.fire("searchQueryChanged", query, context);
            }
        });
    }
});
```

## Context Access

The context map contains:
- All registered components: `context.get("componentId")`
- Selected values: `context.get("selected_componentId")`
- Table models: `context.get("tableModel_componentId")`

You can access the context in event handlers or custom bindings to interact with other components.

## Benefits

1. **Separation of Concerns**: UI rendering is separate from business logic
2. **Reusability**: Renderer works with any entity type
3. **Flexibility**: Default behavior or custom bindings per component
4. **Testability**: Business logic can be tested independently
5. **Maintainability**: Easy to add new events without modifying renderer

