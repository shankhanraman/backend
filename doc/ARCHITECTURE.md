# Arogya Cafe Backend — Architecture & Data Models

## System Overview

**Arogya Cafe** is a Spring Boot backend for a cafe order management and inventory system. It handles:
- **Menu Catalog**: Categories, menu items, and ingredient composition
- **Ordering Workflow**: Customers placing orders → KOTs (Kitchen Order Tickets) → Bills
- **Inventory Management**: Stock tracking, supplier orders, consumption on sale
- **Staff & Security**: JWT-based authentication, role-based access control

---

## Domain Architecture

### 1. **Catalog Domain** (`catalog/`)
Manages the menu and ingredients.

**Entities:**
- **Category**: Groups menu items (e.g., "Beverages", "Snacks")
- **MenuItem**: A product customer can order (name, size variant, price, category)
- **Ingredient**: Base items in inventory (e.g., "Milk", "Sugar")
- **ItemIngredient**: Links menu items to their ingredients with quantity (e.g., Coffee needs 50ml Milk)

**Key Tables:**
```
category (id, name, created_at, updated_at)
ingredient (id, name, unit, created_at, updated_at)
menu_item (id, name, size_variant, price, category_id, created_at, updated_at)
item_ingredient (id, menu_item_id, ingredient_id, quantity, unit, size_variant, created_at, updated_at)
```

**Flow:**
1. Admin creates categories and menu items
2. Admin defines which ingredients go into each menu item
3. Catalog is consumed by the ordering system

---

### 2. **Ordering Domain** (`ordering/`)
Handles the complete order lifecycle from creation to payment.

**Entities:**
- **Customer**: Person placing the order
- **Order**: Top-level request linking customer to lines, KOT, and bill
- **OrderLine**: Individual items in an order (menu_item, quantity, size variant, unit price snapshot)
- **Kot**: Kitchen Order Ticket—sent to kitchen, tracks preparation status
- **Bill**: Invoice for the order, tracks payment status
- **OrderStatus**: CREATED → SERVED → COMPLETED
- **KotStatus**: PENDING → PREPARED → FULFILLED
- **PaymentStatus**: UNPAID → PAID

**Key Tables:**
```
customer (id, name, contact, created_at, updated_at)
orders (id, customer_id, status, created_at, updated_at)
order_line (id, order_id, menu_item_id, size_variant, quantity, unit_price, created_at, updated_at)
kot (id, order_id, status, issued_at, created_at, updated_at)
bill (id, order_id, total_amount, payment_status, billed_at, created_at, updated_at)
order_staff (order_id, staff_id) -- Many-to-many: cashier, server
kot_staff (kot_id, staff_id)     -- Many-to-many: kitchen staff fulfilling KOT
```

**Workflow:**

```
Customer → Create Order (cashier input)
             ↓
         Save Order + OrderLines
             ↓
         Auto-generate Bill (UNPAID) & KOT (PENDING)
             ↓
         Customer pays Bill immediately (UNPAID → PAID)
             ↓
         Send KOT to kitchen
             ↓
         Kitchen prepares items (KOT → PREPARED)
             ↓
         Server marks Order as SERVED (deliver to table)
             ↓
         Order → COMPLETED
```

**Service:** `OrderService`
- `createOrder()`: Validates lines, saves order/lines, auto-generates bill & KOT
- `getOrder()`: Retrieves order with lines, KOT, bill
- `listOrders()`: Lists all orders

**Services:** `KotService`, `BillService`
- `updateKotStatus()`: Kitchen marks KOT as prepared
- `updateBillPaymentStatus()`: Customer pays bill

---

### 3. **Inventory Domain** (`inventory/`)
Tracks stock and supplier relationships.

**Entities:**
- **InventoryStock**: Current stock level per ingredient (qty_on_hand, reorder_threshold)
- **StockTransaction**: Audit trail of every stock change (IN: purchase, OUT: consumption)
- **Supplier**: Vendor for ingredients
- **ConsumptionLine**: Tracks ingredient consumption per order

**Key Tables:**
```
inventory_stock (id, ingredient_id, qty_on_hand, reorder_threshold, last_updated, created_at, updated_at)
stock_transaction (id, inventory_stock_id, type, quantity, triggered_by, supplier_id, order_id, created_at, updated_at)
supplier (id, name, contact, created_at, updated_at)
```

**Stock Transaction Types:**
- `PURCHASE`: Stock added from supplier
- `CONSUMPTION`: Stock used when order is fulfilled
- `ADJUSTMENT`: Manual correction

**Service:** `StockService`
- `recordPurchase()`: Add stock from supplier
- `recordConsumption()`: Deduct stock when KOT is fulfilled
- `getStockLevel()`: Check current inventory

**Workflow:**
```
Admin purchases from supplier
         ↓
   Stock added (PURCHASE transaction)
         ↓
Customer orders menu item → KOT marked PREPARED
         ↓
   Ingredients consumed from stock (CONSUMPTION transaction)
         ↓
Alert if qty_on_hand < reorder_threshold
```

---

### 4. **Security Domain** (`security/`)
Handles authentication and staff roles.

**Entities:**
- **Staff**: Employee record (name, role, username, password_hash)
- **StaffRole**: MANAGER, CASHIER, KITCHEN_STAFF, DELIVERY_STAFF

**Key Tables:**
```
staff (id, name, role, username, password_hash, created_at, updated_at)
```

**Authentication Flow:**
```
Staff login (username/password)
         ↓
   JwtAuthFilter validates JWT in Authorization header
         ↓
   StaffUserDetailsService loads staff by username
         ↓
   @CurrentStaffProvider injects current staff into service methods
```

**Services:**
- `AuthController`: Login endpoint
- `JwtService`: Generate, validate JWT tokens
- `StaffUserDetailsService`: Spring Security integration

---

### 5. **Common & Config** (`common/`, `config/`)

**Enums:**
- `OrderStatus`: CREATED, SERVED, COMPLETED
- `KotStatus`: PENDING, PREPARED, FULFILLED
- `PaymentStatus`: UNPAID, PAID
- `StaffRole`: MANAGER, CASHIER, KITCHEN_STAFF, DELIVERY_STAFF

**Error Handling:**
- `NotFoundException`: Entity not found (404)
- `BusinessRuleException`: Logic violation (e.g., empty order)
- `InsufficientStockException`: Stock too low
- `GlobalExceptionHandler`: Centralized REST error response

**Base Class:**
- `BaseEntity`: Common `id`, `createdAt`, `updatedAt` for all entities

---

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           CAFE ORDER SYSTEM                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  CATALOG SETUP (Admin)                                                       │
│  ├─ Create Category (e.g., "Beverages")                                     │
│  ├─ Create Ingredient (e.g., "Milk" in litres)                              │
│  └─ Create MenuItem linking Ingredients (e.g., Coffee = Milk + Beans)       │
│                                   │                                          │
│                                   ↓                                          │
│  INVENTORY SETUP (Admin)                                                     │
│  ├─ Add Supplier (e.g., "Local Dairy")                                      │
│  ├─ Record initial stock purchase → StockTransaction (PURCHASE)             │
│  └─ InventoryStock updated with qty_on_hand                                 │
│                                   │                                          │
│                                   ↓                                          │
│  CUSTOMER ORDER (Cashier)                                                    │
│  ├─ Create Order with 1+ OrderLines (menu items, qty, size)                 │
│  ├─ Bill auto-generated (status=UNPAID, total=sum of line prices)          │
│  └─ KOT auto-generated (status=PENDING) → sent to kitchen                  │
│                                   │                                          │
│                        ┌──────────┴──────────┐                              │
│                        ↓                     ↓                              │
│   KITCHEN (Kitchen Staff)          BILLING (Cashier)                        │
│   ├─ See KOT (PENDING)             ├─ Customer reviews Bill                 │
│   ├─ Prepare items                 ├─ Customer pays                         │
│   └─ Mark KOT PREPARED             └─ Bill (UNPAID → PAID)                 │
│                        │                     │                              │
│                        └──────────┬──────────┘                              │
│                                   ↓                                          │
│  FULFILLMENT (Server)                                                        │
│  ├─ Verify KOT is PREPARED                                                  │
│  ├─ Deliver to customer                                                     │
│  ├─ Mark Order as SERVED                                                    │
│  └─ Record stock consumption → StockTransaction (CONSUMPTION)               │
│                                   │                                          │
│                                   ↓                                          │
│  COMPLETION                                                                   │
│  ├─ Order status: SERVED → COMPLETED                                        │
│  ├─ InventoryStock.qty_on_hand updated (deducted)                          │
│  └─ If qty < reorder_threshold → alert for restock                         │
│                                                                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Database Schema Relationships

```
CATALOG:
  category
    ↑
    │ (many)
    │
  menu_item ←──────────── item_ingredient ────────→ ingredient
    ↑                                                    ↑
    │ (one)                                             │ (one)
    │                                                   │
    └────────────────────────────────────────────────────┘

ORDERING:
  customer
    ↑
    │ (one)
    │
   order ─────→ kot ────────┐
    │              ↑        │
    │              │ (many) │ (many)
    │              │        │
    │              └──────┬─┴──── kot_staff → staff
    │                     │          ↓
    │                   order_staff (many)
    │                     │
    └─→ order_line → menu_item
    │
    └─→ bill

INVENTORY:
  supplier
    ↑
    │ (one)
    │
  stock_transaction → inventory_stock ← ingredient
  │                                         ↑
  │ (triggered by order_id)                │ (one)
  │                                        │
  └─────────────────────────────────────────┘
```

---

## Key Business Rules

1. **Order Creation**: Must have at least one OrderLine
2. **Bill Calculation**: Total = sum of (OrderLine.quantity × OrderLine.unitPrice)
3. **KOT Workflow**: Can only mark PREPARED when all items are ready
4. **Stock Consumption**: Deducted only after KOT is PREPARED and Order is SERVED
5. **Reorder Alert**: If qty_on_hand < reorder_threshold, flag for purchasing
6. **Staff Roles**: Only appropriate roles can perform actions (e.g., only CASHIER can create orders)
7. **Unique Constraints**:
   - One KOT per Order
   - One Bill per Order
   - Ingredient name is unique
   - Category name is unique
   - Staff username is unique

---

### 6. **Bill Upload Domain** (`supplier/`)
Integrates FastAPI Bill Scanner to automatically process supplier invoices.

**Entities:**
- **BillScannerClient**: HTTP client to FastAPI `/scan` endpoint
- **BillProcessingService**: Converts extracted bill data into supplier purchases

**Key Tables Affected:**
```
supplier (created/updated from vendor info)
ingredient (created for new line items)
inventory_stock (quantities updated)
stock_transaction (PURCHASE records created)
```

**Workflow:**
```
Upload Bill (image/PDF)
         ↓
FastAPI scans & extracts (vendor, items, prices, qty)
         ↓
Find/create Supplier
         ↓
For each line item:
  ├─ Find/create Ingredient
  ├─ Get/create InventoryStock
  ├─ Create StockTransaction (PURCHASE)
  └─ Update qty_on_hand
         ↓
Return ProcessedBillResponse
```

**Integration Point:**
- Calls external FastAPI service running on `http://127.0.0.1:8000`
- Uses three OCR engines: GLM-OCR (best), Tesseract (fast), Gemini (cloud)
- Fully offline-capable with GLM-OCR or Tesseract

**See Also:** `doc/BILL_UPLOAD_INTEGRATION.md` for detailed setup and usage

---

## API Endpoints by Domain

**Catalog:**
- `GET /categories`, `POST /categories`, `GET /categories/{id}`
- `GET /ingredients`, `POST /ingredients`, `GET /ingredients/{id}`
- `GET /menu-items`, `POST /menu-items`, `GET /menu-items/{id}`

**Ordering:**
- `POST /customers`, `GET /customers`, `GET /customers/{id}`
- `POST /orders`, `GET /orders/{id}`, `GET /orders`
- `PUT /kots/{id}/status`, `GET /kots/{id}`
- `PUT /bills/{id}/payment-status`, `GET /bills/{id}`

**Inventory:**
- `GET /suppliers`, `POST /suppliers`
- `GET /inventory/{ingredientId}`, `POST /inventory/purchase`, `POST /inventory/consume`
- `GET /stock-transactions`

**Bill Upload (FastAPI Integration):**
- `POST /bill-upload/process` — Upload & process supplier invoice (auto-extracts vendor, items, creates purchases)
- `GET /bill-upload/health` — Check Bill Scanner service health

**Security:**
- `POST /auth/login`

---

## Technologies

- **Framework**: Spring Boot 3.5.6
- **Database**: PostgreSQL 16
- **ORM**: Hibernate/JPA
- **Migrations**: Flyway
- **Security**: JWT (Spring Security)
- **Build**: Maven
- **API Docs**: OpenAPI/Swagger

---

## For Future Developers

When adding features:
1. **Identify the domain** (Catalog, Ordering, Inventory, Security)
2. **Check existing entities** in that domain for reuse
3. **Follow the transaction pattern** (transactional services, read-only queries)
4. **Respect the workflow** (don't break Order → KOT → Bill flow)
5. **Log stock transactions** for every inventory change
6. **Validate business rules** before saving (see `OrderService.createOrder()`)
7. **Update tests** alongside feature code
