# CinemaBook — Phase 1: Foundation

Cinema streaming platform + social media.  
Backend: **Java 21 + Spring Boot 3.2**  
Frontend: **React 18 + TypeScript + Vite**

---

## Phase 1 delivers

- Full database schema (PostgreSQL via Flyway migrations)
- User registration — Watcher and Film Owner types
- JWT authentication with access + refresh token rotation
- **Single-device session enforcement** via Redis
- SOLID-principled domain interfaces (CLOSED for modification)
- Role-based route protection (frontend + backend)
- Login, registration, and dashboard pages (React)

---

## Project structure

```
cinemabook/
├── docker-compose.yml          # PostgreSQL + Redis for local dev
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/cinemabook/
│       ├── CinemaBookApplication.java
│       ├── config/             SecurityConfig
│       ├── domain/             Core interfaces (UserAccount, FilmOwner,
│       │                       Watcher, PaymentProvider, StreamingProvider)
│       ├── infrastructure/
│       │   ├── persistence/    JPA entities + repositories
│       │   └── security/       JwtService, SessionService, JwtAuthFilter
│       ├── application/
│       │   └── service/        AuthService
│       ├── api/
│       │   ├── controller/     AuthController
│       │   └── dto/            AuthDtos (records)
│       └── shared/             ApiResponse, exceptions, HashUtil
│
└── frontend/
    └── src/
        ├── types/              TypeScript domain types
        ├── services/           apiClient (Axios), authService
        ├── store/              authStore (Zustand)
        ├── components/auth/    ProtectedRoute
        └── pages/
            ├── auth/           LoginPage, RegisterWatcherPage,
            │                   RegisterFilmOwnerPage
            └── dashboard/      WatcherDashboard, OwnerDashboard
```

---

## SOLID principles applied

| Principle | Where |
|-----------|-------|
| **S** — Single Responsibility | `JwtService` only handles tokens. `SessionService` only handles Redis sessions. `AuthService` only orchestrates auth. Controllers only handle HTTP. |
| **O** — Open/Closed | `PaymentProvider`, `StreamingProvider`, `UserAccount` interfaces are closed for modification. New providers/types extend them. |
| **L** — Liskov Substitution | `FilmOwner` and `Watcher` both honour the `UserAccount` contract. `ProtectedRoute` works for any role. |
| **I** — Interface Segregation | `FilmOwner` and `Watcher` are separate interfaces. Neither has methods it doesn't need. |
| **D** — Dependency Inversion | `AuthService` depends on repository interfaces, not concrete classes. Security config injects `JwtAuthFilter` as an abstraction. |

---

## How to run locally

### Prerequisites
- Java 21
- Node 20+
- Docker Desktop

### 1. Set up local configuration and infrastructure

Copy `backend/.env.example` to `backend/.env`. Set a local PostgreSQL password
and a random JWT signing secret of at least 32 bytes. Use plain `NAME=value`
lines, without surrounding quotes. Keep `STRIPE_MOCK=true` for a local demo.
Run Docker Compose from the folder containing `docker-compose.yml`.
```bash
docker compose --env-file backend/.env up -d
# Starts PostgreSQL on :5432 and Redis on :6379
```

### 2. Run Spring Boot backend
```bash
cd backend
./mvnw spring-boot:run
# API available at http://localhost:8082/api/v1
# Flyway runs migrations automatically on startup
```

### 3. Run React frontend
```bash
cd frontend
npm install
npm run dev
# App available at http://localhost:3000
```

---

## API endpoints (Phase 1)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/auth/register/watcher` | Public | Register watcher |
| POST | `/api/v1/auth/register/film-owner` | Public | Register film owner |
| POST | `/api/v1/auth/login` | Public | Login (returns JWT + sets device session) |
| POST | `/api/v1/auth/refresh` | Public | Rotate refresh token |
| POST | `/api/v1/auth/logout` | Bearer | Revoke all tokens + session |
| GET  | `/api/v1/auth/verify-email?token=` | Public | Email verification (Phase 7) |

### Example: Register a watcher
```bash
curl -X POST http://localhost:8082/api/v1/auth/register/watcher \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "username": "alice",
    "password": "supersecret",
    "displayName": "Alice"
  }'
```

### Example: Login
```bash
curl -X POST http://localhost:8082/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "login": "alice@example.com",
    "password": "supersecret",
    "deviceId": "browser-uuid-here",
    "deviceInfo": "Chrome on macOS"
  }'
```

---

## Environment variables (backend)

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_USERNAME` | `cinemabook` | PostgreSQL username |
| `DB_PASSWORD` | Required | PostgreSQL password, also used by Docker Compose |
| `REDIS_HOST` | `localhost` | Redis host |
| `JWT_SECRET` | Required | Random JWT signing secret, at least 32 bytes |
| `CORS_ORIGINS` | `http://localhost:3000` | Allowed frontend origins |
| `STRIPE_MOCK` | `true` | Use mock payment flows locally |
| `STRIPE_SECRET_KEY` | Empty | Reserved for Stripe configuration |
| `TEST_DB_URL` | `jdbc:postgresql://localhost:5432/cinemabook_db_test` | Separate database for integration tests |

Spring Boot reads `backend/.env` when started with `backend/` as its working
directory. Real environment variables can override it. The database container
uses `DB_PASSWORD` when it first initialises its data volume. If a volume already
exists, use its current local database password or change it in PostgreSQL.

---

## Next: Phase 2 — Film Owner Side

- Film upload to S3/R2
- Ticket pricing per film
- Stripe Connect bank linking
- Film management dashboard
