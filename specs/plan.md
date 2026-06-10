# Arogya Cafe — Inventory Management Backend · Living Spec

> This file is the source of truth for the build. It is updated at the **start of every turn**:
> mark completed steps, record decisions/changes, and note what's next.

**Stack:** **Java 21** · Spring Boot 3.5.x · Spring Data JPA · Spring Security (JWT) · Flyway · PostgreSQL (H2 for tests) · **Maven**. (Originally written in Kotlin, then rewritten to Java on request — DTOs as records, entities as plain JPA classes.)
**Package root:** `com.arogya.cafe`

**Toolchain note (host):** JDK **25** + Maven 3.9.11 + Docker 28.4 present; no Gradle CLI. → Build tool changed **Gradle → Maven** (Maven is installed; avoids hand-bootstrapping a Gradle wrapper jar). Kotlin pinned to 2.2.x and `jvmTarget=21` for JDK-25-host compatibility. Tests run on **H2** (no external Postgres needed); Postgres via docker-compose only for manual run.

---

## Progress tracker

| # | Step | Status |
|---|------|--------|
| 0 | Living spec (`specs/plan.md`) created | ✅ done |
| 1 | Project scaffold (Maven, deps, app yml, docker-compose) | ✅ done (compile verified on JDK25→JVM21) |
| 2 | Common: BaseEntity, enums, error handling | ✅ done |
| 3 | Catalog domain (Category, MenuItem+price, Ingredient, ItemIngredient) | ✅ done (compiles) |
| 4 | Inventory domain (Stock, Transaction, Supplier, StockService) | ✅ done (compiles) |
| 5 | Ordering + workflow (Order/Line/KOT/Bill services) | ✅ done (compiles) |
| 6 | Security (Staff principal, JWT, role gates) | ✅ done (compiles) |
| 7 | Flyway migrations + seed (worked-example data) | ✅ done (schema validates on H2) |
| 8 | Tests (workflow integration + authz) | ✅ done (8/8 green) |
| 9 | Docs (README + Swagger UI) | ✅ done |

**BUILD COMPLETE** — all 9 steps done. 12/12 tests pass; worked example verified end-to-end over HTTP against PostgreSQL.

**JAVA REWRITE (later request):** Entire codebase translated Kotlin→Java (Java 21). Same architecture/packages/schema/behavior. DTOs → Java records (nested in `*Dtos` containers with `from()`/`build()` factories); entities → plain JPA classes with getters/setters + id-based equals/hashCode on BaseEntity; Kotlin sources & plugins removed from pom. One fix needed: catalog/inventory controllers map entities→DTOs outside the (open-in-view=false) transaction, so the single-valued associations responses need (MenuItem.category, ItemIngredient.menuItem/ingredient, InventoryStock.ingredient, StockTransaction.inventoryStock/supplier) are now `FetchType.EAGER`. Result: 12/12 tests green; worked example re-verified over HTTP on Postgres (identical numbers, chef-serve 403, etc.).

**Changelog**
- T1: Spec created.
- T2: Switched Gradle→Maven; scaffolded pom/app/yml/compose; compile verified (Spring Boot 3.5.6 + Kotlin 2.2.0 on host JDK 25, jvmTarget 21).
- T3: Built common layer + catalog domain. Added Gap 5 (ItemIngredient.sizeVariant). Compiles clean.
- T4: Built inventory domain — deduction engine + restock + low-stock alerts. Compiles.
- T7/T8: Wrote V1 schema (TIMESTAMP WITH TIME ZONE for Instants) + DataSeeder + OpenApiConfig. Fixed a real bug: entities needed id-based equals/hashCode (BaseEntity) or ManyToMany Set re-adds violated order_staff PK. Tests green; schema validates under Hibernate `validate` on H2.
- T9: README + Swagger (springdoc) done. Live verification against PostgreSQL:
  - Host had native Postgres on 5432 AND 5433 → moved the container to 127.0.0.1:**55432** (docker-compose + application-dev.yml).
  - Authorization: moved workflow role gates from method-level @PreAuthorize to **URL-level rules** in SecurityConfig (cleaner/auditable; @EnableMethodSecurity removed). Added **positive** authz tests (authorized role reaches handler → 404, not 403) — the original deny-only tests couldn't catch an "everyone denied" regression.
  - Diagnosed a misleading 403 on prepare/serve/pay: it was the **smoke-test curl using GET** (no body) on POST-only endpoints → 405 → /error forward → masked 403. Real app was fine. Added `/error` to permitAll so unhandled errors surface their true status instead of a masked 403. Documented the `-X POST` gotcha in README.
  - Final: 12/12 tests green; full worked example verified over HTTP on Postgres (order→KOT/Bill, chef prepare deducts Premix 50→49 & Milk 20000→19820 with CONSUMED txn, role-gated serve [chef 403 / server SERVED], cashier pay → COMPLETED, restock → 29820). **Build complete.**
- T6: Built security — StaffUserDetailsService, JwtService (jjwt HS256), JwtAuthFilter, AuthController (/login, /me), SecurityConfig (stateless, @EnableMethodSecurity, permit auth/swagger/health). Compiles. Next: Flyway schema + seed (step 7).
- T5: Built ordering domain — Customer/Order/OrderLine/Kot/Bill entities (order_staff & kot_staff join tables; OrderLine snapshots unit_price), repos, DTOs/mappers, OrderService (create→auto Bill+KOT, serve), KotService (prepare→consumeForOrder), BillService (pay→complete). Staff entity added (security pkg) + CurrentStaffProvider. Controllers carry @PreAuthorize role gates. Compiles. Next: security wiring — JWT login + SecurityConfig (step 6).

---

## Data model (13 entities)

- **Catalog:** Category(name) → MenuItem(name, size_variant, **price**) ; Ingredient(name, unit) ; ItemIngredient(quantity, unit) [MenuItem↔Ingredient junction].
- **Inventory:** InventoryStock(qty_on_hand, reorder_threshold, last_updated) 1:1 Ingredient ; StockTransaction(type, quantity, triggered_by, created_at) ; Supplier(name, contact).
- **Ordering:** Customer(name, contact) ; Order(status, created_at) ; OrderLine(size_variant, quantity) ; Kot(status, issued_at) 1:1 Order ; Bill(total_amount, payment_status, billed_at) 1:1 Order ; Staff(name, role, **username, password_hash**).

**Enums:** OrderStatus(CREATED→SERVED→COMPLETED), KotStatus(PENDING→PREPARED), PaymentStatus(UNPAID→PAID), StockTransactionType(CONSUMED, RESTOCKED), StaffRole(CHEF, CASHIER, MANAGER, SERVER).

## Business rules
1. Order needs ≥1 OrderLine before a KOT is issued.
2. KOT issued only after Order/Bill created by Cashier.
3. Order → served only after KOT → prepared.
4. KOT prepared → auto-deduct ingredient qty per line; each deduction = CONSUMED StockTransaction linked to Order.
5. Supplier delivery → add stock + RESTOCKED StockTransaction.
6. qty_on_hand ≤ reorder_threshold → low-stock alert.
7. Roles: Cashier/Manager create/modify Bills; Chef sets KOT prepared; Server sets Order served.

## Gaps / decisions
1. Added `price: BigDecimal` to MenuItem (ERD lacks it; Bills need a total — Rs 180 in worked example).
2. Staff doubles as auth principal: add `username` + `passwordHash`, map `role` → authority.
3. Staff↔Order and Staff↔KOT modelled as join tables (`order_staff`, `kot_staff`).
4. Keep both `ItemIngredient.unit` (recipe) and `Ingredient.unit` (base).
5. Added nullable `ItemIngredient.sizeVariant` (not in ERD) so a recipe row can differ per size (null = applies to all sizes). Deduction matches lines where `sizeVariant == line.sizeVariant` OR `sizeVariant IS NULL`. Realises the doc rule "size variants stored as separate Item Ingredient rows".

## Worked example (drives the integration test)
1× Regular Badam Shake (Rs 180): Order created → KOT(PENDING)+Bill(Rs180,UNPAID) auto-generated → Chef prepares → Premix 50→49, Milk 20,000→19,820 ml, 2 CONSUMED txns → Server serves → Cashier pays → Bill PAID, Order COMPLETED. Next-day restock: Milk 19,820→29,820 (RESTOCKED txn).

## Verification
- `docker compose up -d` + `./gradlew bootRun` → Flyway migrates, seed loads.
- `./gradlew test` → all green.
- Replay worked example via Swagger UI / curl (with role-based logins; assert 403 on wrong role).
