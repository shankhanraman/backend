# Data Models Reference

Quick lookup for all entities, their fields, relationships, and constraints.

---

## CATALOG DOMAIN

### Category
Entity for grouping menu items.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | Identity |
| `name` | VARCHAR(255) | NOT NULL, UNIQUE | Display name |
| `createdAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |
| `updatedAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |

**Relations:**
- One-to-many → `MenuItem` (via `category_id`)

**Example:** "Beverages", "Snacks", "Pastries"

---

### Ingredient
Base inventory item (unfinished goods).

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | Identity |
| `name` | VARCHAR(255) | NOT NULL, UNIQUE | e.g., "Milk", "Coffee Beans" |
| `unit` | VARCHAR(255) | NOT NULL | e.g., "litres", "grams", "units" |
| `createdAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |
| `updatedAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |

**Relations:**
- One-to-many → `ItemIngredient` (via `ingredient_id`)
- One-to-one → `InventoryStock` (via `ingredient_id`)

**Example:**
```json
{"name": "Whole Milk", "unit": "litres"}
{"name": "Espresso Beans", "unit": "grams"}
```

---

### MenuItem
Finished product a customer can order.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | Identity |
| `name` | VARCHAR(255) | NOT NULL | e.g., "Cappuccino" |
| `sizeVariant` | VARCHAR(255) | NOT NULL | e.g., "Small", "Medium", "Large" |
| `price` | NUMERIC(10, 2) | NOT NULL | Selling price |
| `categoryId` | BIGINT | NOT NULL, FK | References `Category` |
| `createdAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |
| `updatedAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |

**Relations:**
- Many-to-one → `Category`
- One-to-many → `ItemIngredient`
- One-to-many → `OrderLine`

**Index:** `idx_menu_item_category`

**Example:**
```json
{
  "name": "Cappuccino",
  "sizeVariant": "Medium",
  "price": 5.50,
  "categoryId": 1
}
```

---

### ItemIngredient
Composition: which ingredients go into a menu item and how much.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | Identity |
| `menuItemId` | BIGINT | NOT NULL, FK | References `MenuItem` |
| `ingredientId` | BIGINT | NOT NULL, FK | References `Ingredient` |
| `quantity` | NUMERIC(12, 3) | NOT NULL | Amount needed |
| `unit` | VARCHAR(255) | NOT NULL | Unit of quantity (copied from ingredient) |
| `sizeVariant` | VARCHAR(255) | NULLABLE | If ingredient varies by size (e.g., "Large" needs more milk) |
| `createdAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |
| `updatedAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |

**Relations:**
- Many-to-one → `MenuItem`
- Many-to-one → `Ingredient`

**Index:** `idx_item_ingredient_menu_item`

**Example:**
```json
{
  "menuItemId": 5,
  "ingredientId": 2,
  "quantity": 250,
  "unit": "millilitres",
  "sizeVariant": "Medium"
}
```

---

## ORDERING DOMAIN

### Customer
Person who places orders.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | Identity |
| `name` | VARCHAR(255) | NOT NULL | Customer name |
| `contact` | VARCHAR(255) | NOT NULL | Phone or email |
| `createdAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |
| `updatedAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |

**Relations:**
- One-to-many → `Order` (via `customer_id`)

**Example:**
```json
{"name": "Alice Smith", "contact": "+1-555-0123"}
```

---

### Order
Top-level order request from a customer.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | Identity |
| `customerId` | BIGINT | NOT NULL, FK | References `Customer` |
| `status` | VARCHAR(20) | NOT NULL | Enum: CREATED, SERVED, COMPLETED |
| `createdAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |
| `updatedAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |

**Relations:**
- Many-to-one → `Customer`
- One-to-many → `OrderLine` (via `order_id`)
- One-to-one → `Kot` (via `order_id`)
- One-to-one → `Bill` (via `order_id`)
- Many-to-many → `Staff` (via `order_staff`: cashier, delivery)

**Status Lifecycle:**
```
CREATED → SERVED → COMPLETED
```

**Example:**
```json
{
  "id": 101,
  "customerId": 5,
  "status": "CREATED",
  "createdAt": "2026-06-10T10:00:00Z"
}
```

---

### OrderLine
Individual item in an order.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | Identity |
| `orderId` | BIGINT | NOT NULL, FK | References `Order` |
| `menuItemId` | BIGINT | NOT NULL, FK | References `MenuItem` |
| `sizeVariant` | VARCHAR(255) | NOT NULL | Snapshot of size at order time |
| `quantity` | INTEGER | NOT NULL | How many |
| `unitPrice` | NUMERIC(10, 2) | NOT NULL | Snapshot of price at order time |
| `createdAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |
| `updatedAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |

**Relations:**
- Many-to-one → `Order`
- Many-to-one → `MenuItem`

**Index:** `idx_order_line_order`

**Derived Field:** `lineTotal = quantity × unitPrice`

**Example:**
```json
{
  "id": 501,
  "orderId": 101,
  "menuItemId": 5,
  "sizeVariant": "Medium",
  "quantity": 2,
  "unitPrice": 5.50
}
```

---

### Kot (Kitchen Order Ticket)
Kitchen preparation ticket linked to an Order.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | Identity |
| `orderId` | BIGINT | NOT NULL, UNIQUE, FK | References `Order` |
| `status` | VARCHAR(20) | NOT NULL | Enum: PENDING, PREPARED, FULFILLED |
| `issuedAt` | TIMESTAMP WITH TZ | NOT NULL | When KOT was sent to kitchen |
| `createdAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |
| `updatedAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |

**Relations:**
- One-to-one → `Order`
- Many-to-many → `Staff` (via `kot_staff`: kitchen staff preparing)

**Status Lifecycle:**
```
PENDING → PREPARED → FULFILLED
```

**Business Rules:**
- Created automatically when Order is created
- Sent to kitchen immediately
- Kitchen staff updates status to PREPARED when items are ready
- Cannot mark Order as SERVED until KOT is PREPARED

**Example:**
```json
{
  "id": 201,
  "orderId": 101,
  "status": "PREPARED",
  "issuedAt": "2026-06-10T10:00:30Z"
}
```

---

### Bill
Invoice for an Order.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | Identity |
| `orderId` | BIGINT | NOT NULL, UNIQUE, FK | References `Order` |
| `totalAmount` | NUMERIC(12, 2) | NOT NULL | Sum of OrderLine totals |
| `paymentStatus` | VARCHAR(20) | NOT NULL | Enum: UNPAID, PAID |
| `billedAt` | TIMESTAMP WITH TZ | NOT NULL | When bill was presented |
| `createdAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |
| `updatedAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |

**Relations:**
- One-to-one → `Order`

**Payment Status Lifecycle:**
```
UNPAID → PAID
```

**Calculation:** `totalAmount = sum(OrderLine.quantity × OrderLine.unitPrice)`

**Business Rules:**
- Created automatically when Order is created
- Cannot move Order to COMPLETED until Bill is PAID

**Example:**
```json
{
  "id": 301,
  "orderId": 101,
  "totalAmount": 11.00,
  "paymentStatus": "UNPAID",
  "billedAt": "2026-06-10T10:00:30Z"
}
```

---

## INVENTORY DOMAIN

### InventoryStock
Current stock level for each ingredient.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | Identity |
| `ingredientId` | BIGINT | NOT NULL, UNIQUE, FK | References `Ingredient` |
| `qtyOnHand` | NUMERIC(14, 3) | NOT NULL | Current stock quantity |
| `reorderThreshold` | NUMERIC(14, 3) | NOT NULL | Alert threshold |
| `lastUpdated` | TIMESTAMP WITH TZ | NOT NULL | When stock last changed |
| `createdAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |
| `updatedAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |

**Relations:**
- One-to-one → `Ingredient`
- One-to-many → `StockTransaction` (audit trail)

**Business Rules:**
- Unique per ingredient
- `qtyOnHand >= 0` (no negative stock)
- Alert if `qtyOnHand < reorderThreshold`

**Example:**
```json
{
  "ingredientId": 2,
  "qtyOnHand": 150.5,
  "reorderThreshold": 50.0,
  "lastUpdated": "2026-06-10T10:00:00Z"
}
```

---

### StockTransaction
Audit log for every stock change.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | Identity |
| `inventoryStockId` | BIGINT | NOT NULL, FK | References `InventoryStock` |
| `type` | VARCHAR(20) | NOT NULL | Enum: PURCHASE, CONSUMPTION, ADJUSTMENT |
| `quantity` | NUMERIC(14, 3) | NOT NULL | Amount added/removed |
| `triggeredBy` | VARCHAR(255) | NOT NULL | e.g., "supplier_purchase", "order_fulfilled", "manual_correction" |
| `supplierId` | BIGINT | NULLABLE, FK | References `Supplier` (if PURCHASE) |
| `orderId` | BIGINT | NULLABLE, FK | References `Order` (if CONSUMPTION) |
| `createdAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |
| `updatedAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |

**Relations:**
- Many-to-one → `InventoryStock`
- Many-to-one → `Supplier` (nullable)
- Many-to-one → `Order` (nullable)

**Indexes:** `idx_stock_txn_stock`, `idx_stock_txn_order`

**Transaction Types:**
- **PURCHASE**: Stock in from supplier (quantity > 0)
- **CONSUMPTION**: Stock out when order is fulfilled (quantity < 0)
- **ADJUSTMENT**: Manual correction (positive or negative)

**Example:**
```json
{
  "inventoryStockId": 10,
  "type": "CONSUMPTION",
  "quantity": -250,
  "triggeredBy": "order_fulfilled",
  "orderId": 101,
  "createdAt": "2026-06-10T10:15:00Z"
}
```

---

### Supplier
Vendor for ingredients.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | Identity |
| `name` | VARCHAR(255) | NOT NULL | Supplier name |
| `contact` | VARCHAR(255) | NOT NULL | Phone or email |
| `createdAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |
| `updatedAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |

**Relations:**
- One-to-many → `StockTransaction` (via `supplier_id`)

**Example:**
```json
{"name": "Local Dairy Co", "contact": "+1-555-0456"}
```

---

## SECURITY DOMAIN

### Staff
Employee record with authentication.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `id` | BIGINT | PK, AUTO | Identity |
| `name` | VARCHAR(255) | NOT NULL | Staff member name |
| `role` | VARCHAR(20) | NOT NULL | Enum: MANAGER, CASHIER, KITCHEN_STAFF, DELIVERY_STAFF |
| `username` | VARCHAR(255) | NOT NULL, UNIQUE | Login username |
| `passwordHash` | VARCHAR(255) | NOT NULL | Bcrypt hash |
| `createdAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |
| `updatedAt` | TIMESTAMP WITH TZ | NOT NULL | UTC instant |

**Relations:**
- Many-to-many → `Order` (via `order_staff`: who handled the order)
- Many-to-many → `Kot` (via `kot_staff`: who prepared the KOT)

**Roles:**
- **MANAGER**: Full access (create staff, view reports)
- **CASHIER**: Create orders, receive payments
- **KITCHEN_STAFF**: View/update KOTs
- **DELIVERY_STAFF**: Mark orders as served

**Example:**
```json
{
  "id": 1,
  "name": "Bob Johnson",
  "role": "CASHIER",
  "username": "bob.johnson",
  "passwordHash": "$2a$10$..."
}
```

---

## ENUMS

### OrderStatus
Lifecycle of an Order.

```
CREATED  → Initial state when order is placed
SERVED   → Customer received the order
COMPLETED → Order process finished (payment done, fulfilled)
```

### KotStatus
Lifecycle of a Kitchen Order Ticket.

```
PENDING    → KOT sent to kitchen, waiting for preparation
PREPARED   → All items are ready in kitchen
FULFILLED  → Items delivered to customer
```

### PaymentStatus
Payment state of a Bill.

```
UNPAID → Bill issued, waiting for payment
PAID   → Payment received, order can be completed
```

### StaffRole
Authorization roles.

```
MANAGER        → Full system access
CASHIER        → Create orders, process payments
KITCHEN_STAFF  → Prepare items, update KOT status
DELIVERY_STAFF → Deliver orders, mark as served
```

### StockTransactionType
Type of inventory movement.

```
PURCHASE     → Stock added from supplier
CONSUMPTION  → Stock deducted when order fulfilled
ADJUSTMENT   → Manual correction
```

---

## BILL UPLOAD DOMAIN (FastAPI Integration)

### BillData
Extracted structured data from a scanned bill/invoice.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `vendorName` | STRING | Can be null | Supplier name extracted from bill |
| `vendorContact` | STRING | Can be null | Phone or email of supplier |
| `billNumber` | STRING | Can be null | Invoice/bill number |
| `billDate` | STRING | Can be null | Date from bill (format varies) |
| `currency` | STRING | Can be null | Currency code (optional) |
| `lineItems` | List<LineItem> | NOT NULL | Individual items from bill |
| `subtotal` | DECIMAL | Can be null | Subtotal before tax |
| `total` | DECIMAL | NOT NULL | Total bill amount |
| `paymentMethod` | STRING | Can be null | e.g., "cash", "card", "check" |

**Example:**
```json
{
  "vendorName": "Local Dairy Co",
  "vendorContact": "9123456789",
  "billNumber": "INV_001",
  "billDate": "10/06/26",
  "lineItems": [
    {"description": "Whole Milk", "quantity": 10, "unit_price": 400, "amount": 4000},
    {"description": "Cheese", "quantity": 5, "unit_price": 600, "amount": 3000}
  ],
  "total": 7000
}
```

---

### LineItem
Individual product line from a scanned invoice.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| `description` | STRING | NOT NULL | Item name/description |
| `quantity` | INTEGER | > 0 | Quantity ordered |
| `unitPrice` | DECIMAL | NOT NULL | Price per unit |
| `amount` | DECIMAL | Can be null | Total line amount (qty × price) |

**Used in:** BillData.lineItems

**Example:**
```json
{
  "description": "Whole Milk (litre)",
  "quantity": 10,
  "unit_price": 400,
  "amount": 4000
}
```

---

### ScanResponse
API response from FastAPI Bill Scanner `/scan` endpoint.

| Field | Type | Notes |
|-------|------|-------|
| `success` | BOOLEAN | Whether scan succeeded |
| `engine_used` | STRING | Which OCR engine was used: "glmocr", "tesseract", "gemini" |
| `data` | BillData | Extracted invoice data |
| `warnings` | List<STRING> | OCR confidence warnings (optional) |

**Example:**
```json
{
  "success": true,
  "engine_used": "glmocr",
  "data": { ... },
  "warnings": []
}
```

---

### ProcessedBillResponse
Backend response after processing bill into supplier purchases.

| Field | Type | Notes |
|-------|------|-------|
| `engine` | STRING | OCR engine that scanned the bill |
| `supplier` | Supplier | Created/found supplier from vendor info |
| `billNumber` | STRING | Bill invoice number |
| `billDate` | STRING | Bill date |
| `totalAmount` | DECIMAL | Total bill amount |
| `lineItems` | List<ProcessedLineItem> | Created stock transactions |
| `warnings` | List<STRING> | OCR/processing warnings |

**Example:**
```json
{
  "engine": "glmocr",
  "supplier": {
    "id": 5,
    "name": "Local Dairy Co",
    "contact": "9123456789"
  },
  "billNumber": "INV_001",
  "billDate": "10/06/26",
  "totalAmount": 7000.00,
  "lineItems": [
    {
      "ingredientName": "Whole Milk",
      "unit": "units",
      "quantity": 10,
      "unitPrice": 400.00,
      "newStockLevel": 110.0,
      "transactionId": 2001
    }
  ],
  "warnings": []
}
```

---

### ProcessedLineItem
Item processed from bill into inventory stock transaction.

| Field | Type | Notes |
|-------|------|-------|
| `ingredientName` | STRING | Name of ingredient created/found |
| `unit` | STRING | Unit of measurement (e.g., "units", "kg", "litres") |
| `quantity` | INTEGER | Quantity added to stock |
| `unitPrice` | DECIMAL | Price per unit from bill |
| `newStockLevel` | DECIMAL | Updated qty_on_hand after purchase |
| `transactionId` | LONG | ID of created StockTransaction (PURCHASE) |

**Used in:** ProcessedBillResponse.lineItems

**Example:**
```json
{
  "ingredientName": "Whole Milk",
  "unit": "units",
  "quantity": 10,
  "unitPrice": 400.00,
  "newStockLevel": 110.0,
  "transactionId": 2001
}
```

---

### HealthResponse
Health status of FastAPI Bill Scanner service.

| Field | Type | Notes |
|-------|------|-------|
| `status` | BOOLEAN | Service is operational |
| `message` | STRING | Status message |
| `engines` | EngineStatus | Availability of each OCR engine |

**EngineStatus:**
| Field | Type | Notes |
|-------|------|-------|
| `glmocr_available` | BOOLEAN | Local GLM-OCR model server running (most accurate, ~40s) |
| `tesseract_available` | BOOLEAN | Tesseract offline OCR available (fast, 1-3s) |
| `gemini_available` | BOOLEAN | Google Gemini vision API available (requires internet/key) |

**Example:**
```json
{
  "status": true,
  "message": "All systems operational",
  "engines": {
    "glmocr_available": true,
    "tesseract_available": true,
    "gemini_available": false
  }
}
```

---

## Key Constraints & Validations

### Database Constraints
| Table | Constraint | Reason |
|-------|-----------|--------|
| `ingredient` | `name` UNIQUE | Prevent duplicate ingredients |
| `category` | `name` UNIQUE | Prevent duplicate categories |
| `menu_item` | FK `category_id` NOT NULL | Every item must be in a category |
| `item_ingredient` | FK `menu_item_id`, `ingredient_id` NOT NULL | Composition must reference both |
| `inventory_stock` | `ingredient_id` UNIQUE | One stock record per ingredient |
| `order_line` | FK `order_id`, `menu_item_id` NOT NULL | Lines must belong to order and item |
| `kot` | `order_id` UNIQUE | One KOT per order |
| `bill` | `order_id` UNIQUE | One bill per order |
| `staff` | `username` UNIQUE | Unique login credentials |

### Business Rule Validations (Application Layer)
| Rule | Location | Error |
|------|----------|-------|
| Order must have ≥1 line | `OrderService.createOrder()` | `BusinessRuleException` |
| KOT can only be marked PREPARED if all lines ready | `KotService.updateKotStatus()` | `BusinessRuleException` |
| Order can only be SERVED if KOT is PREPARED | `OrderService.serve()` | `BusinessRuleException` |
| Stock cannot go negative | `StockService.recordConsumption()` | `InsufficientStockException` |
| Bill can only be PAID once | `BillService.updatePaymentStatus()` | `BusinessRuleException` |

---

## Common Queries

**Find all orders with their bills and KOTs:**
```sql
SELECT o.*, b.*, k.*
FROM orders o
LEFT JOIN bill b ON o.id = b.order_id
LEFT JOIN kot k ON o.id = k.order_id
WHERE o.customer_id = ?;
```

**Get ingredients and quantities for a menu item:**
```sql
SELECT i.name, ii.quantity, ii.unit
FROM item_ingredient ii
JOIN ingredient i ON ii.ingredient_id = i.id
WHERE ii.menu_item_id = ?;
```

**Check low stock items:**
```sql
SELECT i.name, s.qty_on_hand, s.reorder_threshold
FROM inventory_stock s
JOIN ingredient i ON s.ingredient_id = i.id
WHERE s.qty_on_hand < s.reorder_threshold;
```

**Stock transaction history for an ingredient:**
```sql
SELECT st.*
FROM stock_transaction st
JOIN inventory_stock s ON st.inventory_stock_id = s.id
WHERE s.ingredient_id = ?
ORDER BY st.created_at DESC;
```
