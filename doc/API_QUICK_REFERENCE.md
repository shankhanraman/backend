# API Quick Reference Card

**Print this & share with frontend team!**

---

## 🔗 Base URL
```
http://localhost:8080
```

---

## 🔐 Authentication Flow

```
POST /auth/login
Body: { "username": "demo", "password": "demo123" }
Response: { "token": "eyJ...", "staff": {...} }

Then add to all requests:
Header: Authorization: Bearer {token}
```

---

## 📍 Essential Endpoints

### Catalog
| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/categories` | GET | ❌ | List categories |
| `/categories` | POST | ❌ | Create category |
| `/menu-items` | GET | ❌ | List menu items |
| `/menu-items` | POST | ❌ | Create menu item |
| `/ingredients` | GET | ❌ | List ingredients |
| `/ingredients` | POST | ❌ | Create ingredient |

### Customers & Orders
| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/customers` | GET | ❌ | List customers |
| `/customers` | POST | ❌ | Create customer |
| `/orders` | POST | ✅ | **Create order** |
| `/orders` | GET | ✅ | List orders |
| `/orders/{id}` | GET | ✅ | Get order details |

### Kitchen & Billing
| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/kots/{id}/status` | PUT | ✅ | Update KOT status |
| `/bills/{id}/payment-status` | PUT | ✅ | Mark bill as paid |
| `/orders/{id}/status` | PUT | ✅ | Serve order |

### Inventory
| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/suppliers` | GET | ❌ | List suppliers |
| `/suppliers` | POST | ❌ | Create supplier |
| `/inventory/{id}` | GET | ❌ | Check stock level |
| `/stock-transactions` | GET | ❌ | View audit trail |

### Bill Upload ⭐
| Endpoint | Method | Auth | Purpose |
|----------|--------|------|---------|
| `/bill-upload/process` | POST | ❌ | Upload & scan bill |
| `/bill-upload/health` | GET | ❌ | Check scanner status |

---

## 📝 Create Order (Most Important)

```
POST /orders
Authorization: Bearer {token}
Content-Type: application/json

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

**Returns:**
```json
{
  "order": { "id": 101, "status": "CREATED" },
  "lines": [...],
  "kot": { "id": 201, "status": "PENDING" },
  "bill": { "id": 301, "totalAmount": 11.00, "paymentStatus": "PAID" }
}
```

**Note:** Payment is processed immediately. Bill automatically PAID when order created.

---

## 🖼️ Upload Bill Image

```
POST /bill-upload/process
Content-Type: multipart/form-data

file: [image.jpg]
engine: "tesseract"
```

**Returns:** Supplier, ingredients, stock updated automatically!

---

## 🎯 Order Workflow

```
1. GET /menu-items           ← Show menu
2. GET /customers            ← Pick customer
3. POST /orders              ← Create order
   ↓ Auto-creates KOT & Bill
4. PUT /bills/{id}/payment-status → Mark PAID
5. PUT /orders/{id}/status → Mark SERVED
   ↓ Stock consumed automatically
```

---

## ❌ Error Codes

| Code | Meaning |
|------|---------|
| 200 | Success |
| 201 | Created |
| 400 | Bad request (validation error) |
| 401 | Unauthorized (missing token) |
| 403 | Forbidden (wrong role) |
| 404 | Not found |
| 503 | Service unavailable |

---

## 💾 Database Tables

- `supplier` — Vendors
- `ingredient` — Stock items
- `menu_item` — Products for sale
- `category` — Menu sections
- `customer` — Buyers
- `orders` — Orders
- `order_line` — Items in order
- `kot` — Kitchen ticket
- `bill` — Invoice
- `inventory_stock` — Current stock
- `stock_transaction` — Stock audit trail

---

## 🔗 Docs & Tools

| Resource | URL |
|----------|-----|
| **Swagger UI** | http://localhost:8080/swagger-ui.html |
| **Backend Health** | http://localhost:8080/actuator/health |
| **Full Docs** | `/doc/FRONTEND_HANDOFF.md` |
| **Architecture** | `/doc/ARCHITECTURE.md` |
| **Data Models** | `/doc/DATA_MODELS.md` |

---

## ⚡ Quick Test Commands

### Login
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo","password":"demo123"}'
```

### List Categories
```bash
curl http://localhost:8080/categories
```

### Create Order
```bash
curl -X POST http://localhost:8080/orders \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"lines":[{"menuItemId":100,"quantity":2}]}'
```

---

## 🛠️ JavaScript Setup

```javascript
const API = 'http://localhost:8080';

// Login once, store token
const login = async () => {
  const res = await fetch(`${API}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'demo', password: 'demo123' })
  });
  const data = await res.json();
  localStorage.setItem('token', data.token);
};

// Use token in requests
const apiCall = (endpoint, method = 'GET', body = null) => {
  const token = localStorage.getItem('token');
  return fetch(`${API}${endpoint}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: body ? JSON.stringify(body) : null
  }).then(r => r.json());
};

// Examples
apiCall('/categories');                    // GET categories
apiCall('/orders', 'POST', orderData);    // Create order
```

---

## 🎬 Roles & Permissions

| Role | Can Do |
|------|--------|
| CASHIER | Create orders, pay bills |
| KITCHEN_STAFF | Update KOT status |
| DELIVERY_STAFF | Mark orders served |
| MANAGER | Everything |

---

## 📱 Environment Variables

```
REACT_APP_API_URL=http://localhost:8080
```

---

**Questions? Check `/doc/FRONTEND_HANDOFF.md` for details!**
