# Data Flow & API Reference

Complete request/response examples and workflows for all major operations.

---

## 1. CATALOG SETUP WORKFLOW

### 1.1 Create a Category

**Endpoint:** `POST /categories`

**Request:**
```json
{
  "name": "Beverages"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "name": "Beverages",
  "createdAt": "2026-06-10T08:00:00Z",
  "updatedAt": "2026-06-10T08:00:00Z"
}
```

---

### 1.2 Create an Ingredient

**Endpoint:** `POST /ingredients`

**Request:**
```json
{
  "name": "Whole Milk",
  "unit": "litres"
}
```

**Response (201 Created):**
```json
{
  "id": 10,
  "name": "Whole Milk",
  "unit": "litres",
  "createdAt": "2026-06-10T08:05:00Z",
  "updatedAt": "2026-06-10T08:05:00Z"
}
```

---

### 1.3 Create a Menu Item

**Endpoint:** `POST /menu-items`

**Request:**
```json
{
  "name": "Cappuccino",
  "sizeVariant": "Medium",
  "price": 5.50,
  "categoryId": 1
}
```

**Response (201 Created):**
```json
{
  "id": 100,
  "name": "Cappuccino",
  "sizeVariant": "Medium",
  "price": 5.50,
  "categoryId": 1,
  "createdAt": "2026-06-10T08:10:00Z",
  "updatedAt": "2026-06-10T08:10:00Z"
}
```

---

### 1.4 Link Ingredient to Menu Item

**Endpoint:** `POST /menu-items/{id}/ingredients`

**Request:**
```json
{
  "ingredientId": 10,
  "quantity": 0.25,
  "sizeVariant": "Medium"
}
```

**Note:** `unit` is auto-populated from `Ingredient.unit`

**Response (201 Created):**
```json
{
  "id": 500,
  "menuItemId": 100,
  "ingredientId": 10,
  "quantity": 0.25,
  "unit": "litres",
  "sizeVariant": "Medium"
}
```

---

## 2. INVENTORY SETUP WORKFLOW

### 2.1 Create a Supplier

**Endpoint:** `POST /suppliers`

**Request:**
```json
{
  "name": "Local Dairy Co",
  "contact": "+1-555-0123"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "name": "Local Dairy Co",
  "contact": "+1-555-0123",
  "createdAt": "2026-06-10T08:15:00Z",
  "updatedAt": "2026-06-10T08:15:00Z"
}
```

---

### 2.2 Record Initial Stock Purchase

**Endpoint:** `POST /inventory/purchase`

**Request:**
```json
{
  "ingredientId": 10,
  "quantity": 100.0,
  "supplierId": 1,
  "reorderThreshold": 20.0
}
```

**Response (200 OK):**
```json
{
  "inventoryStockId": 50,
  "ingredientId": 10,
  "qtyOnHand": 100.0,
  "reorderThreshold": 20.0,
  "lastUpdated": "2026-06-10T08:20:00Z",
  "transaction": {
    "id": 1000,
    "type": "PURCHASE",
    "quantity": 100.0,
    "supplierId": 1,
    "triggeredBy": "supplier_purchase"
  }
}
```

**Side Effect:** `StockTransaction` created with type=PURCHASE

---

## 3. ORDER WORKFLOW

### 3.1 Register a Customer

**Endpoint:** `POST /customers`

**Request:**
```json
{
  "name": "Alice Smith",
  "contact": "+1-555-0999"
}
```

**Response (201 Created):**
```json
{
  "id": 5,
  "name": "Alice Smith",
  "contact": "+1-555-0999",
  "createdAt": "2026-06-10T10:00:00Z",
  "updatedAt": "2026-06-10T10:00:00Z"
}
```

---

### 3.2 Create an Order (Cashier)

**Endpoint:** `POST /orders`

**Request:**
```json
{
  "customerId": 5,
  "lines": [
    {
      "menuItemId": 100,
      "sizeVariant": "Medium",
      "quantity": 2
    }
  ]
}
```

**Context:** Requires JWT with role=CASHIER

**Response (201 Created):**
```json
{
  "order": {
    "id": 101,
    "customerId": 5,
    "status": "CREATED",
    "handledBy": ["cashier_bob"],
    "createdAt": "2026-06-10T10:00:00Z"
  },
  "lines": [
    {
      "id": 501,
      "orderId": 101,
      "menuItemId": 100,
      "sizeVariant": "Medium",
      "quantity": 2,
      "unitPrice": 5.50
    }
  ],
  "kot": {
    "id": 201,
    "orderId": 101,
    "status": "PENDING",
    "issuedAt": "2026-06-10T10:00:30Z"
  },
  "bill": {
    "id": 301,
    "orderId": 101,
    "totalAmount": 11.00,
    "paymentStatus": "UNPAID",
    "billedAt": "2026-06-10T10:00:30Z"
  }
}
```

**Side Effects:**
1. `Order` created with status=CREATED
2. `OrderLine` created for each line with price snapshot
3. `Bill` auto-generated with totalAmount (calculated from order lines)
4. `Kot` auto-generated with status=PENDING
5. KOT sent to kitchen (display on kitchen screen)

**Business Rules Validated:**
- Order must have ≥1 line
- Customer must exist
- Menu item must exist

---

### 3.3 Kitchen Updates KOT Status

**Endpoint:** `PUT /kots/{id}/status`

**Request:**
```json
{
  "status": "PREPARED"
}
```

**Context:** Requires JWT with role=KITCHEN_STAFF

**Response (200 OK):**
```json
{
  "id": 201,
  "orderId": 101,
  "status": "PREPARED",
  "issuedAt": "2026-06-10T10:00:30Z",
  "preparedAt": "2026-06-10T10:10:00Z",
  "updatedAt": "2026-06-10T10:10:00Z"
}
```

**Business Rules Validated:**
- KOT status can only transition: PENDING → PREPARED → FULFILLED
- Kitchen staff must be listed in `kot_staff` table

---

### 3.4 Payment Processing

**Endpoint:** `PUT /bills/{id}/payment-status`

**Request:**
```json
{
  "paymentStatus": "PAID"
}
```

**Context:** Requires JWT with role=CASHIER

**Response (200 OK):**
```json
{
  "id": 301,
  "orderId": 101,
  "totalAmount": 11.00,
  "paymentStatus": "PAID",
  "billedAt": "2026-06-10T10:00:30Z",
  "paidAt": "2026-06-10T10:12:00Z",
  "updatedAt": "2026-06-10T10:12:00Z"
}
```

**Business Rules Validated:**
- Bill can only transition: UNPAID → PAID

---

### 3.5 Delivery & Order Completion

**Endpoint:** `PUT /orders/{id}/status`

**Request:**
```json
{
  "status": "SERVED"
}
```

**Context:** Requires JWT with role=DELIVERY_STAFF or CASHIER

**Response (200 OK):**
```json
{
  "id": 101,
  "customerId": 5,
  "status": "SERVED",
  "handledBy": ["cashier_bob", "server_alice"],
  "updatedAt": "2026-06-10T10:13:00Z"
}
```

**Side Effects:**
1. For each `OrderLine` in the order:
   - Calculate ingredient consumption from `ItemIngredient` composition
   - Deduct from `InventoryStock.qtyOnHand`
   - Create `StockTransaction` with type=CONSUMPTION
2. Order status changes to SERVED
3. KOT status changes to FULFILLED

**Business Rules Validated:**
- Order status can only transition: CREATED → SERVED → COMPLETED
- KOT must be PREPARED before order can be SERVED
- Bill must be PAID before order can be SERVED

---

### 3.6 Example: Complete Order Data Retrieval

**Endpoint:** `GET /orders/{id}`

**Response (200 OK):**
```json
{
  "order": {
    "id": 101,
    "customerId": 5,
    "customer": {
      "id": 5,
      "name": "Alice Smith",
      "contact": "+1-555-0999"
    },
    "status": "SERVED",
    "handledBy": ["bob_johnson", "alice_smith"],
    "createdAt": "2026-06-10T10:00:00Z",
    "updatedAt": "2026-06-10T10:13:00Z"
  },
  "lines": [
    {
      "id": 501,
      "orderId": 101,
      "menuItem": {
        "id": 100,
        "name": "Cappuccino",
        "sizeVariant": "Medium",
        "price": 5.50,
        "category": {
          "id": 1,
          "name": "Beverages"
        }
      },
      "sizeVariant": "Medium",
      "quantity": 2,
      "unitPrice": 5.50,
      "lineTotal": 11.00
    }
  ],
  "kot": {
    "id": 201,
    "status": "FULFILLED",
    "issuedAt": "2026-06-10T10:00:30Z",
    "preparedAt": "2026-06-10T10:10:00Z",
    "fulfilledAt": "2026-06-10T10:13:00Z"
  },
  "bill": {
    "id": 301,
    "totalAmount": 11.00,
    "paymentStatus": "PAID",
    "billedAt": "2026-06-10T10:00:30Z",
    "paidAt": "2026-06-10T10:12:00Z"
  }
}
```

---

## 4. INVENTORY MANAGEMENT WORKFLOW

### 4.1 Check Stock Level

**Endpoint:** `GET /inventory/{ingredientId}`

**Response (200 OK):**
```json
{
  "id": 50,
  "ingredientId": 10,
  "ingredient": {
    "id": 10,
    "name": "Whole Milk",
    "unit": "litres"
  },
  "qtyOnHand": 75.5,
  "reorderThreshold": 20.0,
  "lastUpdated": "2026-06-10T10:13:00Z",
  "requiresReorder": false
}
```

**Note:** `requiresReorder = qtyOnHand < reorderThreshold`

---

### 4.2 View Stock Transactions (Audit Trail)

**Endpoint:** `GET /stock-transactions?ingredientId={id}&limit=20`

**Response (200 OK):**
```json
[
  {
    "id": 1001,
    "inventoryStockId": 50,
    "type": "CONSUMPTION",
    "quantity": -0.5,
    "triggeredBy": "order_fulfilled",
    "order": {
      "id": 101,
      "customerId": 5
    },
    "createdAt": "2026-06-10T10:13:00Z"
  },
  {
    "id": 1000,
    "inventoryStockId": 50,
    "type": "PURCHASE",
    "quantity": 100.0,
    "triggeredBy": "supplier_purchase",
    "supplier": {
      "id": 1,
      "name": "Local Dairy Co"
    },
    "createdAt": "2026-06-10T08:20:00Z"
  }
]
```

---

### 4.3 Low Stock Alert

**Endpoint:** `GET /inventory/low-stock`

**Response (200 OK):**
```json
[
  {
    "id": 50,
    "ingredientId": 10,
    "ingredient": {
      "name": "Whole Milk"
    },
    "qtyOnHand": 15.0,
    "reorderThreshold": 20.0,
    "urgency": "HIGH"
  },
  {
    "id": 51,
    "ingredientId": 11,
    "ingredient": {
      "name": "Sugar"
    },
    "qtyOnHand": 8.0,
    "reorderThreshold": 10.0,
    "urgency": "CRITICAL"
  }
]
```

---

## 5. BILL UPLOAD & SUPPLIER PURCHASE WORKFLOW

### 5.1 Upload & Process Supplier Invoice

**Endpoint:** `POST /bill-upload/process`

**Request (multipart form):**
```
file: [bill.jpg or invoice.pdf]
engine: "auto"  // or "glmocr", "tesseract", "gemini"
```

**Request (curl):**
```bash
curl -X POST "http://localhost:8080/bill-upload/process" \
  -F "file=@invoice.jpg" \
  -F "engine=auto"
```

**Processing Pipeline:**
```
1. Upload bill image/PDF
2. Call FastAPI Bill Scanner /scan endpoint
3. OCR engine extracts text (GLM-OCR → Tesseract fallback)
4. LLM structurer parses into JSON:
   - vendor_name, vendor_contact
   - bill_number, bill_date
   - line_items: [description, quantity, unit_price]
   - total_amount
5. Backend processes:
   - Find/create Supplier (by vendor_name)
   - For each line item:
     ├─ Find/create Ingredient
     ├─ Get/create InventoryStock
     ├─ Create StockTransaction (PURCHASE, type)
     └─ Update qty_on_hand
6. Return ProcessedBillResponse
```

**Response (201 Created):**
```json
{
  "engine": "glmocr",
  "supplier": {
    "id": 5,
    "name": "Local Dairy Co",
    "contact": "9123456789",
    "createdAt": "2026-06-10T14:00:00Z"
  },
  "billNumber": "INV_001",
  "billDate": "10/06/26",
  "totalAmount": 5000.00,
  "lineItems": [
    {
      "ingredientName": "Whole Milk",
      "unit": "units",
      "quantity": 10,
      "unitPrice": 400.00,
      "newStockLevel": 110.0,
      "transactionId": 2001
    },
    {
      "ingredientName": "Cheese",
      "unit": "units",
      "quantity": 5,
      "unitPrice": 600.00,
      "newStockLevel": 65.0,
      "transactionId": 2002
    }
  ],
  "warnings": []
}
```

**Side Effects:**
1. Supplier created or updated
2. Ingredients auto-created for new items
3. StockTransaction records created (type=PURCHASE)
4. InventoryStock.qty_on_hand updated for each ingredient
5. Full audit trail in stock_transaction table

**Business Rules Validated:**
- Bill must have a vendor name
- Line items must have descriptions and quantities > 0
- Quantities are positive (added to stock, not consumed)

---

### 5.2 Example: Real Supplier Invoice

**Physical bill received:**
```
┌─────────────────────────────────┐
│   Local Dairy Co                 │
│   Phone: 9123456789              │
│                                  │
│   Invoice #INV_001               │
│   Date: 10/06/26                 │
│                                  │
│   Whole Milk (litre)    10  × 400│
│   Cheese (kg)            5  × 600│
│                                  │
│   TOTAL: ₹ 5,000                │
└─────────────────────────────────┘
```

**Step 1: Take photo & upload**
```powershell
POST /bill-upload/process
-F "file=@bill_photo.jpg"
```

**Step 2: FastAPI extracts:**
```json
{
  "vendor_name": "Local Dairy Co",
  "vendor_contact": "9123456789",
  "bill_number": "INV_001",
  "bill_date": "10/06/26",
  "line_items": [
    {"description": "Whole Milk", "quantity": 10, "unit_price": 400},
    {"description": "Cheese", "quantity": 5, "unit_price": 600}
  ],
  "total": 5000
}
```

**Step 3: Backend creates in database:**

**suppliers table:**
```
id | name             | contact      | created_at
5  | Local Dairy Co   | 9123456789   | 2026-06-10T14:00:00Z
```

**ingredient table:**
```
id | name             | unit   | created_at
20 | Whole Milk       | units  | 2026-06-10T14:00:15Z
21 | Cheese           | units  | 2026-06-10T14:00:15Z
```

**inventory_stock table:**
```
id  | ingredient_id | qty_on_hand | reorder_threshold | last_updated
51  | 20            | 110.0       | 10.0              | 2026-06-10T14:00:30Z
52  | 21            | 65.0        | 5.0               | 2026-06-10T14:00:30Z
```

**stock_transaction table:**
```
id   | inventory_stock_id | type       | quantity | supplier_id | triggered_by  | created_at
2001 | 51                 | RESTOCKED  | 10       | 5           | bill_upload   | 2026-06-10T14:00:30Z
2002 | 52                 | RESTOCKED  | 5        | 5           | bill_upload   | 2026-06-10T14:00:30Z
```

**Step 4: Response confirms:**
- Supplier "Local Dairy Co" created with contact
- 2 ingredients created
- 2 stock transactions recorded
- Stock levels updated (ready for orders)

---

### 5.3 Check Bill Scanner Health

**Endpoint:** `GET /bill-upload/health`

**Response (200 OK):**
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

**Indicates:**
- ✅ GLM-OCR server running (most accurate, ~40s)
- ✅ Tesseract available (fast fallback, 1-3s)
- ❌ Gemini requires internet (free tier limited)

---

## 6. SECURITY & AUTHENTICATION

### 6.1 Staff Login

**Endpoint:** `POST /auth/login`

**Request:**
```json
{
  "username": "bob.johnson",
  "password": "SecurePass123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "staff": {
    "id": 1,
    "name": "Bob Johnson",
    "role": "CASHIER",
    "username": "bob.johnson"
  },
  "expiresIn": "480 minutes"
}
```

---

### 6.2 Using JWT Token

**All Authenticated Requests:**

```
Header: Authorization: Bearer <token>
```

**Example:**
```bash
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  http://localhost:8080/orders
```

---

## 7. ERROR RESPONSES

### 7.1 Business Rule Violation

**Endpoint:** `POST /orders` (with empty lines)

**Request:**
```json
{
  "customerId": 5,
  "lines": []
}
```

**Response (400 Bad Request):**
```json
{
  "timestamp": "2026-06-10T10:14:00Z",
  "status": 400,
  "error": "Business Rule Violation",
  "message": "An order must have at least one line",
  "path": "/orders"
}
```

---

### 7.2 Insufficient Stock

**Endpoint:** `PUT /orders/{id}/status` (to SERVED, but stock too low)

**Response (400 Bad Request):**
```json
{
  "timestamp": "2026-06-10T10:14:00Z",
  "status": 400,
  "error": "Insufficient Stock",
  "message": "Cannot fulfill order: Whole Milk (need 0.5, have 0.2)",
  "path": "/orders/101/status"
}
```

---

### 7.3 Not Found

**Endpoint:** `GET /orders/99999`

**Response (404 Not Found):**
```json
{
  "timestamp": "2026-06-10T10:14:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Order 99999 not found",
  "path": "/orders/99999"
}
```

---

### 7.4 Unauthorized (Missing JWT)

**Endpoint:** `POST /orders` (without Authorization header)

**Response (401 Unauthorized):**
```json
{
  "timestamp": "2026-06-10T10:14:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Missing or invalid JWT token",
  "path": "/orders"
}
```

---

### 7.5 Forbidden (Wrong Role)

**Endpoint:** `PUT /kots/201/status` (as CASHIER, requires KITCHEN_STAFF)

**Response (403 Forbidden):**
```json
{
  "timestamp": "2026-06-10T10:14:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "This action requires role: KITCHEN_STAFF",
  "path": "/kots/201/status"
}
```

---

## 8. STATE MACHINE DIAGRAMS

### Order Workflow
```
┌─────────┐
│ CREATED │ ← Order placed by cashier
└────┬────┘
     │ (KOT prepared in kitchen)
     ↓
┌─────────┐
│ SERVED  │ ← Customer received order, stock consumed
└────┬────┘
     │ (Bill paid)
     ↓
┌───────────┐
│ COMPLETED │ ← Order process finished
└───────────┘
```

### KOT Workflow
```
┌─────────┐
│ PENDING │ ← KOT issued to kitchen
└────┬────┘
     │ (Items prepared)
     ↓
┌─────────┐
│ PREPARED│ ← Ready for delivery
└────┬────┘
     │ (Delivered to customer)
     ↓
┌───────────┐
│ FULFILLED │ ← Completed
└───────────┘
```

### Bill Workflow
```
┌────────┐
│ UNPAID │ ← Bill issued
└────┬───┘
     │ (Payment received)
     ↓
┌─────┐
│ PAID│ ← Order can now be completed
└─────┘
```

---

## 9. Integration Notes for Agents

### Order Creation Sequence
1. **Validate:** Customer, menu items, at least one line
2. **Save Order** with status=CREATED
3. **Save OrderLines** with price snapshot
4. **Calculate Bill total** = sum(line.quantity × line.unitPrice)
5. **Create Bill** with status=UNPAID
6. **Create KOT** with status=PENDING
7. **Return combined response** with order, lines, KOT, bill

### Order Completion Sequence
1. **Check KOT is PREPARED**
2. **Check Bill is PAID**
3. **For each OrderLine:**
   - Look up `ItemIngredient` records for the menu item
   - Calculate total consumption for each ingredient
   - Deduct from `InventoryStock.qtyOnHand`
   - Create `StockTransaction` with type=CONSUMPTION
4. **Update Order status** to SERVED
5. **Update KOT status** to FULFILLED
6. **Check inventory alerts** (qty < threshold)

### Stock Management Sequence
1. **On purchase:** Create StockTransaction (PURCHASE), add to qtyOnHand
2. **On consumption:** Create StockTransaction (CONSUMPTION), subtract from qtyOnHand
3. **Check threshold:** If qtyOnHand < reorderThreshold, include in alerts
4. **Audit trail:** All transactions are immutable (for compliance/debugging)

---

## 10. Common Query Patterns

### "Show me all open orders"
```
SELECT * FROM orders WHERE status = 'CREATED' ORDER BY created_at DESC;
```

### "What ingredients do we need for this menu item?"
```
SELECT i.name, ii.quantity, ii.unit, ii.size_variant
FROM item_ingredient ii
JOIN ingredient i ON ii.ingredient_id = i.id
WHERE ii.menu_item_id = ?
ORDER BY i.name;
```

### "What's our current stock?"
```
SELECT i.name, s.qty_on_hand, s.reorder_threshold,
       CASE WHEN s.qty_on_hand < s.reorder_threshold THEN 'ALERT' ELSE 'OK' END as status
FROM inventory_stock s
JOIN ingredient i ON s.ingredient_id = i.id
ORDER BY s.qty_on_hand ASC;
```

### "How much of each ingredient was consumed today?"
```
SELECT i.name, -SUM(st.quantity) as consumed
FROM stock_transaction st
JOIN inventory_stock inv ON st.inventory_stock_id = inv.id
JOIN ingredient i ON inv.ingredient_id = i.id
WHERE st.type = 'CONSUMPTION'
  AND DATE(st.created_at) = CURRENT_DATE
GROUP BY i.name;
```
