# Backend conventions — Arogya Cafe

Spring Boot 3.5 · Java 21 · Maven · Spring Data JPA · Spring Security (JWT) · Flyway · PostgreSQL.
Package root `com.arogya.cafe`. Build with `./mvnw` (committed wrapper).

## Structure: package-by-feature

Each domain owns its full vertical slice:

```
com.arogya.cafe.<module>.{controller, service, dto, entity, repository}
modules: catalog · inventory · ordering · security · supplier
common: BaseEntity, enums, exceptions, GlobalExceptionHandler
config:  SecurityConfig, OpenApiConfig, DataSeeder
```

Add a feature with the **`new-domain-feature` skill** so the slice stays consistent.

## Hard rules (enforced by tests/hooks — don't fight them)

- **Money & quantities are `BigDecimal`.** ArchUnit (`ArchitectureFitnessTest`) fails the build on any
  `double`/`float` field. `OrderLine` snapshots `unit_price` at order time — bills never recompute from
  the live menu price.
- **DTOs are Java `record`s** nested in `*Dtos` containers with `from()`/`build()` factories.
  Entities are plain JPA classes with getters/setters and id-based `equals/hashCode` (via `BaseEntity`).
- **Map entities → DTOs inside the service / transaction.** `open-in-view=false`, so a lazy association
  touched in a controller will throw. Single-valued associations that responses need are `FetchType.EAGER`
  (see `MenuItem.category`, `InventoryStock.ingredient`, etc.). Prefer mapping in the service layer.
- **Authorization is URL-level** in `SecurityConfig` (method + path → role), not `@PreAuthorize`. Every new
  endpoint needs a matcher there **and** a positive + negative test in `RoleAuthorizationTest`.
- **Stock never goes negative.** `consumeForOrder` checks remaining ≥ 0 and throws `InsufficientStockException`.
  `InventoryStock` carries a `@Version` optimistic lock — concurrent deductions can't lose a write.

## Migrations

Schema lives in `src/main/resources/db/migration/V*.sql`. Immutable once applied. Additive /
expand-contract only. Use the **`flyway-migration` skill**. `ddl-auto: validate` means an entity that
doesn't match the migrated schema fails every integration test — that's intentional.

## Tests

Integration tests extend `support/AbstractIntegrationTest` (one shared PostgreSQL container). They need a
running Docker daemon locally; in CI the runner provides one. `ArchitectureFitnessTest` needs no Docker.
After any DB or entity change, run the worked-example (`OrderWorkflowIntegrationTest`) and
`MigrationConsistencyTest`.
