# Testing guide

How tests are organised, what each layer covers, and where the **341 test count** comes from.

---

## The test pyramid for this project

Tests mirror the four layers of the Clean Architecture (see `CLAUDE.md`). Each
layer uses the lightest test tool that gives confidence in *that* layer's
responsibility — no Spring context where one isn't needed.

| Layer | Test style | Spring? | DB? | What it proves |
|---|---|---|---|---|
| **Domain** | Plain JUnit 5 + AssertJ | No | No | Business rules and invariants enforced inside POJOs (e.g. `Order.cancel` requires MANAGER for non-PLACED states; `Bill` rounds tax to 2 decimals; `User` rejects blank usernames). Fast — runs in milliseconds, no framework bootstrapping. |
| **Infrastructure / persistence** | `@DataJpaTest` + H2 in-memory | Slice (JPA only) | H2 emulating PostgreSQL | Mapper + repository adapters round-trip Domain ↔ JpaEntity correctly, unique constraints hold, finders return expected results. |
| **Infrastructure / security** | Plain JUnit | No | No | `BCryptPasswordHasher` actually hashes and verifies. |
| **Application** | Mockito unit tests (one `@SpringBootTest` integration) | Mocks only | No (except the integration test, which uses H2) | Each service composes Domain calls correctly, enforces cross-aggregate guards (e.g. "table must be OCCUPIED to start an order"), and emits the right `AuditLogEntry`. |
| **Presentation** | `@WebMvcTest` + `@MockitoBean` | Web slice only | No | Each controller URL is reachable by the right role and forbidden (403) for wrong roles. The Application layer's own tests already cover the business logic — controller tests are smoke tests for routing + security. |

The pyramid shape: lots of cheap Domain tests at the base, fewer slow
Spring-loading tests at the top.

---

## Where the 341 number comes from

Counts are taken from `target/surefire-reports/TEST-*.xml` after
`./mvnw test`.

### Domain — **248 tests** across 11 classes

| Class | Tests | Notes |
|---|---:|---|
| `OrderTest` | **81** | 22 `@Test` methods, but several are `@ParameterizedTest` with `@EnumSource` / `@CsvSource` (one per `OrderState`, one per illegal transition, etc.) — JUnit expands each parameter set into a separate test run. |
| `BillTest` | 33 | BR3 rounding edges, split-divisor cases, snapshot ownership checks. |
| `UserTest` | 25 | username normalisation, password hash rules, `recordLogin` invariants. |
| `PaymentTest` | 22 | 16 `@Test` methods; a few are parameterised over `Role` to verify BR4 (only MANAGER). |
| `ConfigurationTest` | 21 | Singleton invariants, rate ranges, accepted-methods set rules. |
| `MenuItemTest` | 17 | Price > 0, deactivation, name length. |
| `OrderItemTest` | 11 | `recordedUnitPrice` snapshot, quantity > 0, attach-once. |
| `CategoryTest` | 11 | display order, soft-delete. |
| `AuditLogEntryTest` | 10 | append-only, required fields. |
| `BillLineSnapshotTest` | 9 | record-component validation. |
| `TableTest` | 8 | seat / release / deactivate. |

### Infrastructure / persistence — **48 tests** across 9 adapters

Each adapter (`UserRepositoryImplTest`, `OrderRepositoryImplTest`, …) verifies
that a Domain object can be `save`-d and reloaded via the mapper, that
declared finders return the right rows, and that unique constraints are
enforced. Implemented with `@DataJpaTest + @Import(XRepositoryImpl.class)`
and H2 in-memory.

| Class | Tests |
|---|---:|
| `AuditLogRepositoryImplTest` | 8 |
| `UserRepositoryImplTest` | 7 |
| `BillRepositoryImplTest` | 6 |
| `OrderRepositoryImplTest` | 5 |
| `PaymentRepositoryImplTest` | 5 |
| `MenuItemRepositoryImplTest` | 5 |
| `TableRepositoryImplTest` | 4 |
| `ConfigurationRepositoryImplTest` | 4 |
| `CategoryRepositoryImplTest` | 4 |

### Infrastructure / security — **4 tests**

`BCryptPasswordHasherTest` (4): hash produces BCrypt-format output;
`matches` accepts the original; rejects wrong passwords; rejects rehashed
strings.

### Application — **26 tests** across 7 classes

Unit tests use `@ExtendWith(MockitoExtension.class)` with `@Mock`
repositories and `@InjectMocks` services. Each service has:

- one **happy-path** test per public method, and
- one **failure path** per service (wrong state, missing entity, BR violation).

Audit emission is verified via `ArgumentCaptor<AuditLogEntry>`.

| Class | Tests | Notes |
|---|---:|---|
| `AuthenticationServiceTest` | 6 |
| `PlaceOrderServiceTest` | 6 | one test per method (start, add, remove, submit) + 2 failure paths |
| `OrderLifecycleServiceTest` | 4 |
| `GenerateBillServiceTest` | 3 |
| `RecordPaymentServiceTest` | 3 | Mockito unit test |
| `RecordPaymentServiceIntegrationTest` | 1 | **`@SpringBootTest` + H2** — full round-trip: seed → seat → order → submit → prep → ready → served → bill → pay. Verifies Order=COMPLETED, Table=AVAILABLE, Bill.paid=true, audit entries written. |
| `SeatCustomersServiceTest` | 3 |

### Presentation — **15 tests** across 7 controllers

Each `@WebMvcTest` loads one controller + the security config, and uses
`@MockitoBean` for collaborators. Pattern:

- `200` for the authorised role (`@WithMockUser(roles = "...")`)
- `403` for an unauthorised role

| Class | Tests |
|---|---:|
| `OrderControllerTest` | 4 |
| `BillControllerTest` | 2 |
| `DashboardControllerTest` | 2 |
| `KitchenControllerTest` | 2 |
| `PaymentControllerTest` | 2 |
| `TableControllerTest` | 2 |
| `LoginControllerTest` | 1 |

### Total

248 + 48 + 4 + 26 + 15 = **341**.

---

## How to run

```bash
./mvnw test                                          # all 341 tests
./mvnw test -Dtest=BillTest                          # one class
./mvnw test -Dtest='com.daust.restaurant.domain.*'   # one package
```

Surefire writes per-class XML and TXT reports to `target/surefire-reports/`.
The TXT files show individual test names and timings; the XML files include
parameter expansion (useful to see why `OrderTest` reports 81 runs).

---

## Why parameterised tests inflate the count

`OrderTest` declares ~22 methods but the surefire report shows 81. JUnit's
parameterised mechanism (`@ParameterizedTest` + `@EnumSource`, `@CsvSource`,
`@MethodSource`) **runs the same method body once per input**, and each run
counts as a separate test in the report. This is intentional — every
parameter set is an independent assertion:

```java
@ParameterizedTest
@EnumSource(value = OrderState.class,
            names = {"IN_PREPARATION", "READY", "SERVED"})
void cancel_inActiveState_requiresManager(OrderState state) { ... }
```

That one method becomes three separate test runs (one per state).
PaymentTest does similar over `Role`.

---

## What is NOT tested (by design)

- **End-to-end HTTP flows through the templates.** Manual smoke from the
  browser is the verification path for view rendering (see the manual
  checklist at the end of the presentation-layer commit message).
- **Cancellation (UC09), split/merge (UC15/16), reporting (UC19), audit
  viewer (UC20)** — these features are not implemented yet, so no tests
  exist for them.
- **PostgreSQL-specific behaviour.** Tests run against H2 in PostgreSQL
  compatibility mode (`MODE=PostgreSQL`). Schema is created via Hibernate
  DDL, not from a migration script. Real Postgres is exercised manually.
