# Restaurant Ordering and Table Management System

CS 2712 — Software Design & Architecture (DAUST).
Server-rendered Java 21 / Spring Boot 4 implementation of the design report. Java backend, Thymeleaf views, PostgreSQL, BCrypt auth,
role-based access for Admin / Manager / Waiter / Kitchen Staff.

This README is the **how-to-run** entry point.

---

## Quick start — Docker 

You need **Docker Desktop** (or Docker Engine + Compose v2). Nothing else:
no local Java, no local Postgres.

```bash
docker compose up --build
```

First run takes a few minutes (downloads JDK, builds the jar, pulls
Postgres 16). Then visit **<http://localhost:8080>**.

To stop:

```bash
docker compose down            # keep DB data
docker compose down -v         # also wipe DB data (re-seeds on next start)
```

The compose file launches two services:

| Service | Port | Image |
|---|---|---|
| `app` | `8080` (host) → `8080` (container) | built from `./Dockerfile` |
| `db`  | `5432` (host) → `5432` (container) | `postgres:16` |

The app waits for Postgres to be healthy before starting. On first boot,
`SeedDataService` populates the database (see "Demo accounts" below).

---

## Quick start — without Docker

Requires **JDK 21** and a running **PostgreSQL** (default port 5432).

```bash
# 1. Create the database and user (one-time)
psql -U postgres -c "CREATE DATABASE restaurant_system;"
psql -U postgres -c "CREATE USER restaurant WITH PASSWORD 'restaurant';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE restaurant_system TO restaurant;"

# 2. Tell Spring Boot how to connect
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/restaurant_system
export SPRING_DATASOURCE_USERNAME=restaurant
export SPRING_DATASOURCE_PASSWORD=restaurant
# Optional: enable the prod profile (cached templates, port 8080)
export SPRING_PROFILES_ACTIVE=prod

# 3. Run
./mvnw spring-boot:run
```

If you skip step 2, the dev defaults in `application.properties` are used
(`localhost:5432`, user `postgres`, password `postgres`, port `8081`).

---

## Demo accounts (created automatically on first boot)

| Username  | Password     | Role          | Lands on   |
|-----------|--------------|---------------|------------|
| `admin`   | `admin123`   | ADMIN         | `/dashboard` |
| `manager` | `manager123` | MANAGER       | `/dashboard` |
| `waiter`  | `waiter123`  | WAITER        | `/tables`  |
| `kitchen` | `kitchen123` | KITCHEN_STAFF | `/kitchen` |

Seed data also creates 5 tables (capacities 2/2/4/4/6), 2 categories
(Starters, Mains), and 6 Senegalese menu items.

---

## Walkthrough — happy-path demo

1. **Sign in as `waiter`** → land on `/tables`.
2. Click **Seat** on an AVAILABLE table → it flips to OCCUPIED.
3. Click **New Order** → **Start Order** → add 2 items → **Submit to Kitchen**.
4. Sign out, sign in as **`kitchen`** → land on `/kitchen`. Click
   **Start Preparation**, then **Mark Ready**.
5. Sign out, sign in as **`waiter`** → from the nav bar click **My Orders**
   (`/orders/active`). The order should be READY — click **Mark Served**.
6. Sign out, sign in as **`manager`** → `/dashboard`. Open the served
   order (via the audit trail in the DB, or navigate manually) → click
   **Generate Bill** → preview → **Generate Bill**.
7. On the bill page, click **Record Payment**. Method = `CASH`, amount ≥
   total. Submit → confirmation page shows change due. The Order is
   COMPLETED, the Table back to AVAILABLE.

All ten state transitions (UC03, UC08, UC10, UC11, UC12, UC13, UC14) are
exercised in `RecordPaymentServiceIntegrationTest` — the same flow, end
to end, against an in-memory database.

---

## Build, test, package

```bash
./mvnw test                       # run all 341 tests
./mvnw clean package              # build jar → target/restaurant-system-*.jar
./mvnw spring-boot:run            # run from source
java -jar target/restaurant-system-*.jar   # run the packaged jar
```

The Docker build runs `clean package -DskipTests` inside the build stage;
tests are intended to be run separately (CI or local) before building the
image.

---

## Configuration

All datasource and profile settings can be overridden by environment
variables. Spring Boot maps env var names by uppercasing the property
key and replacing `.` with `_`.

| Env var | Property | Default (dev) |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `spring.datasource.url` | `jdbc:postgresql://localhost:5432/restaurant_system` |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` | `postgres` |
| `SPRING_PROFILES_ACTIVE` | `spring.profiles.active` | _none_ (dev) |
| `SERVER_PORT` | `server.port` | `8081` (dev) / `8080` (prod) |

### Profiles

- **(no profile)** — `application.properties`: dev defaults, port 8081,
  Thymeleaf cache off, `ddl-auto=update`, formatted SQL.
- **`prod`** — also reads `application-prod.properties`: port 8080,
  Thymeleaf cache on, quieter logging.

---

## Project layout

```
src/main/java/com/daust/restaurant/
  domain/          # POJOs + business rules — no framework imports
  application/     # @Service use-case orchestrators, transactions, audit
  infrastructure/  # JPA entities, mappers, repository adapters, security
  presentation/    # @Controller classes (Thymeleaf views)
src/main/resources/
  templates/       # Thymeleaf HTML (Bootstrap 5 from CDN)
  application.properties           # dev defaults
  application-prod.properties      # prod overrides
```

"Architecture — Strict Clean Architecture" for the
dependency rule between layers; it's enforced socially, not by a build
tool.


