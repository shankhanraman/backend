# Frontend Integration Handoff

**Arogya Cafe Backend API** — Complete guide for frontend developers to integrate with the backend.

---

## 🎯 Quick Start

**Base URL:** `http://localhost:8080`

**API Documentation:** `http://localhost:8080/swagger-ui.html`

**Backend Status:** `http://localhost:8080/actuator/health`

---

## 🔐 Authentication

All endpoints except `/auth/login` require JWT token in the header.

### Login & Get Token

**Endpoint:** `POST /auth/login`

**Request:**
```json
{
  "username": "demo",
  "password": "demo123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "staff": {
    "id": 1,
    "name": "Demo User",
    "role": "MANAGER",
    "username": "demo"
  },
  "expiresIn": "480 minutes"
}
```

### Use Token in Requests

Add to all authenticated requests:
```
Authorization: Bearer {token}
```

**Example (JavaScript):**
```javascript
const token = localStorage.getItem('authToken');
const headers = {
  'Authorization': `Bearer ${token}`,
  'Content-Type': 'application/json'
};
```

---

## 📍 All API Endpoints

### **CATALOG ENDPOINTS**

#### Get All Categories
```
GET /categories
```
**Response:**
```json
[
  {
    "id": 1,
    "name": "Beverages",
    "createdAt": "2026-06-10T08:00:00Z",
    "updatedAt": "2026-06-10T08:00:00Z"
  }
]
```

#### Create Category
```
POST /categories
```
**Body:**
```json
{
  "name": "Beverages"
}
```

#### Get All Menu Items
```
GET /menu-items
```
**Response:**
```json
[
  {
    "id": 100,
    "name": "Cappuccino",
    "sizeVariant": "Medium",
    "price": 5.50,
    "categoryId": 1,
    "createdAt": "2026-06-10T08:10:00Z"
  }
]
```

#### Create Menu Item
```
POST /menu-items
```
**Body:**
```json
{
  "name": "Cappuccino",
  "sizeVariant": "Medium",
  "price": 5.50,
  "categoryId": 1
}
```

#### Get All Ingredients
```
GET /ingredients
```

#### Create Ingredient
```
POST /ingredients
```
**Body:**
```json
{
  "name": "Whole Milk",
  "unit": "litres"
}
```

---

### **ORDERING ENDPOINTS**

#### Create Customer
```
POST /customers
```
**Body:**
```json
{
  "name": "Alice Smith",
  "contact": "+1-555-0123"
}
```

#### Get All Customers
```
GET /customers
```

#### Create Order ⭐ (Most Important)
```
POST /orders
Headers: Authorization: Bearer {token}
```
**Body:**
```json
{
  "customerId": 1,
  "lines": [
    {
      "menuItemId": 100,
      "sizeVariant": "Medium",
      "quantity": 2
    }
  ],
  "paymentMethod": "cash"
}
```

**Note:** Payment is processed immediately upon order creation. Bill status automatically becomes PAID.

**Response:**
```json
{
  "order": {
    "id": 101,
    "customerId": 1,
    "status": "CREATED",
    "createdAt": "2026-06-10T10:00:00Z"
  },
  "lines": [
    {
      "id": 501,
      "orderId": 101,
      "menuItemId": 100,
      "quantity": 2,
      "unitPrice": 5.50
    }
  ],
  "kot": {
    "id": 201,
    "status": "PENDING"
  },
  "bill": {
    "id": 301,
    "totalAmount": 11.00,
    "paymentStatus": "PAID"
  }
}
```

#### Get Order Details
```
GET /orders/{orderId}
```

#### Get All Orders
```
GET /orders
```

#### Update KOT Status (Kitchen)
```
PUT /kots/{kotId}/status
Headers: Authorization: Bearer {token}
```
**Body:**
```json
{
  "status": "PREPARED"
}
```

#### Update Bill Payment
```
PUT /bills/{billId}/payment-status
Headers: Authorization: Bearer {token}
```
**Body:**
```json
{
  "paymentStatus": "PAID"
}
```

#### Serve Order (Consume Stock)
```
PUT /orders/{orderId}/status
Headers: Authorization: Bearer {token}
```
**Body:**
```json
{
  "status": "SERVED"
}
```

---

### **INVENTORY ENDPOINTS**

#### Get All Suppliers
```
GET /suppliers
```

#### Create Supplier
```
POST /suppliers
```
**Body:**
```json
{
  "name": "Local Dairy Co",
  "contact": "9876543210"
}
```

#### Get Stock Level
```
GET /inventory/{ingredientId}
```
**Response:**
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

#### Get Stock Transactions (Audit Trail)
```
GET /stock-transactions
```

---

### **BILL UPLOAD ENDPOINTS** ⭐

#### Upload & Process Bill Image
```
POST /bill-upload/process
Headers: Content-Type: multipart/form-data
```
**Parameters:**
- `file` — Bill image (JPG/PNG) or PDF
- `engine` — "auto" | "tesseract" | "glmocr" | "gemini"

**Response:**
```json
{
  "engine": "tesseract",
  "supplier": {
    "id": 5,
    "name": "Local Dairy Co",
    "contact": "9876543210"
  },
  "billNumber": "INV_001",
  "billDate": "10/06/26",
  "totalAmount": 5000.00,
  "lineItems": [
    {
      "ingredientName": "Whole Milk",
      "quantity": 10,
      "unitPrice": 400.00,
      "newStockLevel": 110.0,
      "transactionId": 2001
    }
  ]
}
```

#### Check Bill Scanner Health
```
GET /bill-upload/health
```
**Response:**
```json
{
  "status": true,
  "engines": {
    "glmocr_available": true,
    "tesseract_available": true,
    "gemini_available": false
  }
}
```

---

## 🔴 Error Responses

### 400 Bad Request
```json
{
  "timestamp": "2026-06-10T10:14:00Z",
  "status": 400,
  "error": "Business Rule Violation",
  "message": "An order must have at least one line",
  "path": "/orders"
}
```

### 401 Unauthorized
```json
{
  "timestamp": "2026-06-10T10:14:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Missing or invalid JWT token",
  "path": "/orders"
}
```

### 403 Forbidden
```json
{
  "timestamp": "2026-06-10T10:14:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "This action requires role: KITCHEN_STAFF",
  "path": "/kots/201/status"
}
```

### 404 Not Found
```json
{
  "timestamp": "2026-06-10T10:14:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Order 99999 not found",
  "path": "/orders/99999"
}
```

### 503 Service Unavailable
```json
{
  "status": 503,
  "message": "Bill Scanner service unavailable"
}
```

---

## 📋 Common Integration Flows

### **Flow 1: Create & Complete an Order**

```
1. GET /customers
   ↓
2. GET /menu-items
   ↓
3. POST /orders (with menuItemId, quantity)
   ↓ Returns: orderId, kotId, billId
   ↓
4. PUT /bills/{billId}/payment-status (PAID)
   ↓
5. PUT /orders/{orderId}/status (SERVED)
   ↓
6. GET /orders/{orderId} (verify completed)
```

### **Flow 2: Process Supplier Bill**

```
1. GET /bill-upload/health (verify scanner ready)
   ↓
2. POST /bill-upload/process (upload image)
   ↓ Returns: created supplier, ingredients, stock levels
   ↓
3. GET /inventory/{ingredientId} (verify stock updated)
   ↓
4. GET /stock-transactions (audit trail)
```

### **Flow 3: Check Inventory**

```
1. GET /inventory/low-stock
   ↓
2. For each low item:
   GET /inventory/{ingredientId}
   ↓
3. GET /stock-transactions (see history)
```

---

## 🧪 Testing with cURL

### Login
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo123"}'
```

### Create Order
```bash
curl -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "lines": [{"menuItemId": 100, "sizeVariant": "Medium", "quantity": 2}]
  }'
```

### Upload Bill
```bash
curl -X POST http://localhost:8080/bill-upload/process \
  -F "file=@bill.jpg" \
  -F "engine=tesseract"
```

---

## 🌐 Frontend Implementation Examples

### **JavaScript (Fetch API)**

```javascript
// Login
async function login(username, password) {
  const response = await fetch('http://localhost:8080/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password })
  });
  const data = await response.json();
  localStorage.setItem('authToken', data.token);
  return data;
}

// Get Categories
async function getCategories() {
  const response = await fetch('http://localhost:8080/categories');
  return response.json();
}

// Create Order (requires token)
async function createOrder(customerId, lines) {
  const token = localStorage.getItem('authToken');
  const response = await fetch('http://localhost:8080/orders', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ customerId, lines })
  });
  return response.json();
}

// Upload Bill
async function uploadBill(file) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('engine', 'tesseract');
  
  const response = await fetch('http://localhost:8080/bill-upload/process', {
    method: 'POST',
    body: formData
  });
  return response.json();
}
```

### **React Example**

```jsx
import { useState, useEffect } from 'react';

const API_URL = 'http://localhost:8080';

export function useAuth() {
  const [token, setToken] = useState(localStorage.getItem('authToken'));
  
  const login = async (username, password) => {
    const response = await fetch(`${API_URL}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    const data = await response.json();
    setToken(data.token);
    localStorage.setItem('authToken', data.token);
  };
  
  return { token, login };
}

export function useAPI() {
  const token = localStorage.getItem('authToken');
  
  const call = async (endpoint, options = {}) => {
    const headers = {
      'Content-Type': 'application/json',
      ...options.headers
    };
    
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
    
    const response = await fetch(`${API_URL}${endpoint}`, {
      ...options,
      headers
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message);
    }
    
    return response.json();
  };
  
  return { call };
}
```

---

## 🛠️ Environment Configuration

### Development
```
BACKEND_URL=http://localhost:8080
```

### Production
```
BACKEND_URL=https://cafe-api.example.com
```

---

## 📞 Support & Questions

| Topic | Location |
|-------|----------|
| Full API docs | http://localhost:8080/swagger-ui.html |
| Architecture | `/doc/ARCHITECTURE.md` |
| Data models | `/doc/DATA_MODELS.md` |
| API workflows | `/doc/DATA_FLOW.md` |
| Bill upload | `/doc/BILL_UPLOAD_INTEGRATION.md` |

---

## ✅ Checklist Before Starting

- [ ] Backend running on `http://localhost:8080`
- [ ] PostgreSQL database `arogya_cafe` created
- [ ] Bill Scanner running on `http://127.0.0.1:8000` (optional for bill upload)
- [ ] Can login and get JWT token
- [ ] Can create order
- [ ] Can upload bill image
- [ ] Swagger UI accessible and working

---

**Happy coding! 🚀**
