# Arogya Cafe Backend

**Production-ready Spring Boot café order management system with FastAPI bill scanner integration.**

---

## 🚀 Quick Start

### Prerequisites
- Java 21, PostgreSQL 16, Maven 3.8+

### Setup
```bash
# Create database
psql -U postgres -c "CREATE DATABASE arogya_cafe;"

# Start FastAPI Bill Scanner (optional)
cd path/to/bill-scanner && .\run.ps1

# Run Spring Boot
cd c:\demo\cafe\backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**Backend:** http://localhost:8080  
**API Docs:** http://localhost:8080/swagger-ui.html

---

## 📁 Folder Structure ✅ REORGANIZED

**Clean Layered + Domain Architecture:**

```
src/main/java/com/arogya/cafe/
├── catalog/
│   ├── controller/      ← REST APIs
│   ├── service/         ← Business logic
│   ├── repository/      ← JPA queries
│   ├── entity/          ← Database models
│   └── dto/             ← Request/Response
├── ordering/            (same layered structure)
├── inventory/           (same layered structure)
├── supplier/            ← Bill upload
├── security/            ← Authentication
├── common/              ← Shared (entity, enums, exception)
└── config/              ← Spring configuration
```

**See [FOLDER_STRUCTURE.md](FOLDER_STRUCTURE.md) and [REORGANIZATION_CHECKLIST.md](REORGANIZATION_CHECKLIST.md)**

---

## 📚 Key Documentation

| File | Purpose |
|------|---------|
| [ARCHITECTURE.md](doc/ARCHITECTURE.md) | System design |
| [FRONTEND_HANDOFF.md](doc/FRONTEND_HANDOFF.md) | API reference |
| [API_QUICK_REFERENCE.md](doc/API_QUICK_REFERENCE.md) | Quick lookup |
| [FRONTEND_SETUP.md](doc/FRONTEND_SETUP.md) | Frontend guide |
| [FOLDER_STRUCTURE.md](FOLDER_STRUCTURE.md) | Code organization |
| [REORGANIZATION_CHECKLIST.md](REORGANIZATION_CHECKLIST.md) | Migration guide |

---

## 🔐 Authentication

```bash
POST /auth/login
{
  "username": "demo",
  "password": "demo123"
}
```

Use returned token: `Authorization: Bearer {token}`

---

## 📍 Key Endpoints

**Catalog:** `GET /categories`, `GET /menu-items`, `GET /ingredients`

**Orders:** `POST /orders`, `GET /orders`, `PUT /orders/{id}/status`

**Bill Upload:** `POST /bill-upload/process` ⭐

**Full list:** [FRONTEND_HANDOFF.md](doc/FRONTEND_HANDOFF.md)

---

## 🏗️ Architecture

5 domains with layers:
- **Catalog** - Menu & ingredients
- **Ordering** - Orders & workflow
- **Inventory** - Stock management
- **Supplier** - Bill scanning
- **Security** - Authentication

---

## 📊 Database

PostgreSQL 16 with Flyway migrations

---

**Built with Spring Boot, PostgreSQL & FastAPI**
