# Project Folder Structure

**Recommended Spring Boot Architecture** for Arogya Cafe Backend

---

## 📁 Current Structure (Implemented)

### ✅ Current (Layered + Domain) - NOW IMPLEMENTED
```
src/
├── main/
│   ├── java/com/arogya/cafe/
│   │   ├── domains/                          ← Domain modules
│   │   │   ├── catalog/
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── repository/
│   │   │   │   ├── entity/
│   │   │   │   ├── dto/
│   │   │   │   └── exception/
│   │   │   ├── ordering/
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── repository/
│   │   │   │   ├── entity/
│   │   │   │   ├── dto/
│   │   │   │   └── exception/
│   │   │   ├── inventory/
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── repository/
│   │   │   │   ├── entity/
│   │   │   │   ├── dto/
│   │   │   │   └── exception/
│   │   │   ├── supplier/                     ← Bill upload integration
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── client/
│   │   │   │   ├── dto/
│   │   │   │   └── exception/
│   │   │   └── security/
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── entity/
│   │   │       ├── dto/
│   │   │       ├── filter/
│   │   │       ├── provider/
│   │   │       └── util/
│   │   ├── common/                           ← Shared code
│   │   │   ├── exception/
│   │   │   ├── entity/
│   │   │   ├── enums/
│   │   │   ├── util/
│   │   │   └── constant/
│   │   ├── config/                           ← Spring configuration
│   │   │   ├── SecurityConfig.java
│   │   │   ├── OpenApiConfig.java
│   │   │   ├── RestTemplateConfig.java
│   │   │   └── DataSeeder.java
│   │   └── CafeBackendApplication.java       ← Entry point
│   └── resources/
│       ├── application.yml
│       ├── application-dev.yml
│       ├── db/migration/
│       │   └── V1__schema.sql
│       └── static/
├── test/
│   ├── java/com/arogya/cafe/
│   │   ├── domains/
│   │   │   ├── catalog/
│   │   │   │   ├── CatalogServiceTest.java
│   │   │   │   ├── CatalogControllerTest.java
│   │   │   │   └── CategoryRepositoryTest.java
│   │   │   ├── ordering/
│   │   │   │   ├── OrderServiceTest.java
│   │   │   │   ├── OrderControllerTest.java
│   │   │   │   └── OrderRepositoryTest.java
│   │   │   ├── inventory/
│   │   │   │   ├── StockServiceTest.java
│   │   │   │   └── StockRepositoryTest.java
│   │   │   ├── supplier/
│   │   │   │   ├── BillProcessingServiceTest.java
│   │   │   │   ├── BillScannerClientTest.java
│   │   │   │   └── BillUploadControllerTest.java
│   │   │   └── security/
│   │   │       ├── JwtServiceTest.java
│   │   │       └── AuthControllerTest.java
│   │   └── integration/
│   │       ├── OrderIntegrationTest.java
│   │       ├── BillUploadIntegrationTest.java
│   │       └── EndToEndTest.java
│   └── resources/
│       ├── application-test.yml
│       └── test-data.sql
├── pom.xml
├── README.md
└── doc/
    ├── ARCHITECTURE.md
    ├── DATA_MODELS.md
    ├── DATA_FLOW.md
    ├── BILL_UPLOAD_INTEGRATION.md
    ├── FRONTEND_HANDOFF.md
    ├── API_QUICK_REFERENCE.md
    ├── FRONTEND_SETUP.md
    └── FOLDER_STRUCTURE.md
```

---

## 🏗️ Package Organization by Domain

### **Catalog Domain**
```
src/main/java/com/arogya/cafe/domains/catalog/
├── controller/
│   ├── CategoryController.java
│   ├── MenuItemController.java
│   └── IngredientController.java
├── service/
│   └── CatalogService.java
├── repository/
│   ├── CategoryRepository.java
│   ├── MenuItemRepository.java
│   ├── IngredientRepository.java
│   └── ItemIngredientRepository.java
├── entity/
│   ├── Category.java
│   ├── MenuItem.java
│   ├── Ingredient.java
│   └── ItemIngredient.java
├── dto/
│   └── CatalogDtos.java
└── exception/
    └── CatalogException.java
```

### **Ordering Domain**
```
src/main/java/com/arogya/cafe/domains/ordering/
├── controller/
│   ├── OrderController.java
│   ├── KotController.java
│   ├── BillController.java
│   └── CustomerController.java
├── service/
│   ├── OrderService.java
│   ├── KotService.java
│   └── BillService.java
├── repository/
│   ├── OrderRepository.java
│   ├── OrderLineRepository.java
│   ├── KotRepository.java
│   ├── BillRepository.java
│   └── CustomerRepository.java
├── entity/
│   ├── Order.java
│   ├── OrderLine.java
│   ├── Kot.java
│   ├── Bill.java
│   └── Customer.java
├── dto/
│   └── OrderingDtos.java
└── exception/
    ├── OrderException.java
    └── BillException.java
```

### **Inventory Domain**
```
src/main/java/com/arogya/cafe/domains/inventory/
├── controller/
│   ├── SupplierController.java
│   └── InventoryController.java
├── service/
│   └── StockService.java
├── repository/
│   ├── SupplierRepository.java
│   ├── InventoryStockRepository.java
│   └── StockTransactionRepository.java
├── entity/
│   ├── Supplier.java
│   ├── InventoryStock.java
│   ├── StockTransaction.java
│   └── ConsumptionLine.java
├── dto/
│   └── InventoryDtos.java
└── exception/
    ├── InsufficientStockException.java
    └── StockException.java
```

### **Supplier/Bill Upload Domain**
```
src/main/java/com/arogya/cafe/domains/supplier/
├── controller/
│   └── BillUploadController.java
├── service/
│   └── BillProcessingService.java
├── client/
│   └── BillScannerClient.java
├── dto/
│   ├── ScanResponse.java
│   ├── BillData.java
│   └── ProcessedBillResponse.java
└── exception/
    └── BillScannerException.java
```

### **Security Domain**
```
src/main/java/com/arogya/cafe/domains/security/
├── controller/
│   └── AuthController.java
├── service/
│   ├── JwtService.java
│   └── StaffUserDetailsService.java
├── entity/
│   └── Staff.java
├── repository/
│   └── StaffRepository.java
├── filter/
│   └── JwtAuthFilter.java
├── provider/
│   └── CurrentStaffProvider.java
├── dto/
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   └── StaffDto.java
└── util/
    └── TokenUtil.java
```

### **Common/Shared**
```
src/main/java/com/arogya/cafe/common/
├── entity/
│   └── BaseEntity.java
├── exception/
│   ├── NotFoundException.java
│   ├── BusinessRuleException.java
│   ├── InsufficientStockException.java
│   └── GlobalExceptionHandler.java
├── enums/
│   ├── OrderStatus.java
│   ├── KotStatus.java
│   ├── PaymentStatus.java
│   ├── StaffRole.java
│   └── StockTransactionType.java
├── util/
│   ├── DateUtil.java
│   └── ValidationUtil.java
└── constant/
    └── AppConstants.java
```

### **Configuration**
```
src/main/java/com/arogya/cafe/config/
├── SecurityConfig.java
├── OpenApiConfig.java
├── RestTemplateConfig.java
├── DataSeeder.java
└── ApplicationProperties.java
```

---

## 🧪 Test Folder Structure

Mirror the main structure:

```
src/test/java/com/arogya/cafe/
├── domains/
│   ├── catalog/
│   │   ├── CatalogServiceTest.java
│   │   ├── CatalogControllerTest.java
│   │   └── CategoryRepositoryTest.java
│   ├── ordering/
│   │   ├── OrderServiceTest.java
│   │   ├── OrderControllerTest.java
│   │   └── OrderRepositoryTest.java
│   ├── inventory/
│   │   ├── StockServiceTest.java
│   │   └── StockRepositoryTest.java
│   ├── supplier/
│   │   ├── BillProcessingServiceTest.java
│   │   ├── BillScannerClientTest.java
│   │   └── BillUploadControllerTest.java
│   └── security/
│       ├── JwtServiceTest.java
│       ├── AuthControllerTest.java
│       └── StaffRepositoryTest.java
├── integration/
│   ├── OrderEndToEndTest.java
│   ├── BillUploadIntegrationTest.java
│   └── CatalogIntegrationTest.java
└── fixtures/
    ├── OrderFixture.java
    ├── MenuItemFixture.java
    └── CustomerFixture.java
```

---

## 📝 Naming Conventions

### Controllers
- `{Domain}{Resource}Controller.java`
- Examples: `OrderController`, `CategoryController`, `BillUploadController`

### Services
- `{Domain}Service.java` or `{Resource}Service.java`
- Examples: `OrderService`, `StockService`, `BillProcessingService`

### Repositories
- `{Entity}Repository.java`
- Examples: `OrderRepository`, `CategoryRepository`, `SupplierRepository`

### Entities
- PascalCase, singular noun
- Examples: `Order`, `Category`, `MenuItem`

### DTOs
- `{Resource}Dto.java` or in `{Domain}Dtos.java` file
- Examples: `CreateOrderRequest`, `OrderResponse`, `CategoryDto`

### Exceptions
- `{Domain}Exception.java` or `{Specific}Exception.java`
- Examples: `OrderException`, `InsufficientStockException`, `BillScannerException`

### Tests
- `{ClassName}Test.java`
- Examples: `OrderServiceTest`, `OrderControllerTest`, `OrderRepositoryTest`

---

## 📦 Maven Dependencies Organization

Group by purpose in `pom.xml`:

```xml
<!-- Core Spring -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Data -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Database -->
<dependency>
  <groupId>org.postgresql</groupId>
  <artifactId>postgresql</artifactId>
</dependency>

<!-- Security -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- API Docs -->
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
</dependency>

<!-- Testing -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-test</artifactId>
  <scope>test</scope>
</dependency>

<!-- Utilities -->
<dependency>
  <groupId>org.projectlombok</groupId>
  <artifactId>lombok</artifactId>
  <optional>true</optional>
</dependency>
```

---

## 🚀 Benefits of This Structure

| Aspect | Benefit |
|--------|---------|
| **Domain-Based** | Easy to find related code (all Order code together) |
| **Layered** | Clear separation of concerns (controller → service → repo) |
| **Scalable** | Easy to add new domains without affecting others |
| **Testable** | Mirrored test structure makes testing straightforward |
| **Maintainable** | New developers quickly understand the layout |
| **Parallel Dev** | Teams can work on different domains independently |

---

## 📋 Implementation Checklist

- [ ] Create `domains/` folder structure
- [ ] Move controllers to `{domain}/controller/`
- [ ] Move services to `{domain}/service/`
- [ ] Move repositories to `{domain}/repository/`
- [ ] Move entities to `{domain}/entity/`
- [ ] Move DTOs to `{domain}/dto/`
- [ ] Move exceptions to `{domain}/exception/`
- [ ] Create `test/` folder structure
- [ ] Move tests to mirror `src/main/` structure
- [ ] Update all import statements
- [ ] Verify tests still pass
- [ ] Update documentation (README, etc.)

---

## 📖 Example: Order Creation Flow

Following this structure, creating an order flows through:

```
Request
  ↓
OrderController.java           ← domains/ordering/controller/
  ↓
OrderService.java              ← domains/ordering/service/
  ↓
OrderRepository.java           ← domains/ordering/repository/
OrderLineRepository.java       ← domains/ordering/repository/
KotRepository.java             ← domains/ordering/repository/
BillRepository.java            ← domains/ordering/repository/
  ↓
Database
```

All order-related code is in one domain folder! 🎯

---

**This structure is production-ready and follows Spring Boot best practices!**
