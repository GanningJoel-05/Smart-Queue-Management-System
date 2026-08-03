# 🏥 Smart Queue Management System

A **Spring Boot REST API** that replaces the "take a paper token and wait" hospital/clinic experience with a live, digital queue — patients get a real-time position and estimated wait time, doctors manage their queue with one click, and admins get instant clinic-level stats.

Built to solve a real problem: outpatient departments where nobody knows how long they'll actually be waiting.

---

## ✨ What It Does

- **Digital token booking** — patients book a slot with a doctor and instantly get a queue position and an **estimated wait time (EWT)**, calculated from that doctor's real average consultation time (not a guess).
- **Live queue updates over WebSockets** — no polling. When a patient's turn is approaching, is called, or the queue shifts (no-show, cancellation, urgent case), every affected patient is pushed a real-time update via STOMP/WebSocket.
- **Urgent-case handling** — a doctor can insert an urgent patient into position 2 (right after whoever's currently being seen), and the system automatically shifts and recalculates wait times for everyone behind them.
- **Self-learning wait estimates** — every doctor's average consultation time is recalculated as a running average after each patient, so EWTs get more accurate over time instead of relying on a fixed number.
- **Role-based access control** — three distinct roles (Admin, Doctor, Patient) with JWT authentication and endpoint-level authorization, so a patient can never touch a doctor's queue controls and vice versa.
- **Daily auto-reset** — a scheduled job clears out stale, unresolved tokens at midnight so every clinic starts each day with a clean queue.
- **Admin analytics** — per-clinic stats (queue depth, busiest doctor, per-doctor breakdown) for admins managing multiple clinics.

---

## 🧱 Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 17 |
| Framework | Spring Boot 3.2 |
| Data Access | Spring Data JPA (Hibernate) |
| Database | PostgreSQL |
| Auth | Spring Security + JWT (JJWT) |
| Real-time | Spring WebSocket (STOMP over SockJS) |
| API Docs | springdoc-openapi (Swagger UI) |
| Validation | Jakarta Bean Validation |
| Build Tool | Maven |
| Boilerplate Reduction | Lombok |

---

## 🏗️ How It's Built

The project follows a clean, layered architecture that separates concerns and mirrors how production Spring Boot services are structured:

```
controller/    → REST endpoints, request/response handling, role-based access
service/       → Business logic (queue engine, auth, stats)
repository/    → Spring Data JPA interfaces for persistence
entity/        → JPA-mapped domain models (User, Clinic, Doctor, Token)
dto/           → Request/response objects, decoupled from entities
security/      → JWT filter, user details service, security config
websocket/     → STOMP config + broadcaster for live queue events
scheduler/     → Cron job for the midnight queue reset
exception/     → Centralized exception handling with custom domain exceptions
```

### The queue engine, in short

Each doctor's queue is a live, ordered list of tokens. The core logic (`QueueEngineService`) handles four situations:

1. **New booking** → assign the next open queue position, calculate initial EWT.
2. **Urgent insert** → place the patient at position 2, shift everyone else down, recalculate and broadcast new wait times.
3. **Consultation done / no-show / cancellation** → shift the whole queue up by one, recalculate EWTs, and push targeted WebSocket events ("your turn is coming up" at position 2, "you're up" at position 1).
4. **Post-consultation** → update the doctor's running average consultation time, which feeds back into every future EWT calculation for that doctor.

---

## 🔌 API Overview

All endpoints are prefixed with `/api`. Full interactive documentation is available via Swagger UI once the app is running.

| Resource | Endpoints | Access |
|---|---|---|
| **Auth** | Register (patient / doctor / admin), Login | Public registration for patients & admins; doctor registration is admin-only |
| **Clinics** | Create, list/search, get by ID, update, deactivate, list doctors | Public read, admin-only write |
| **Doctors** | View today's queue, mark consulted, mark no-show, update status, view stats | Doctor / Admin only |
| **Tokens** | Book a token, get token details, cancel, booking history | Patient only (view shared with doctor/admin) |
| **Admin** | Clinic-level stats, list managed clinics | Admin only |

Every protected endpoint is secured with a JWT bearer token and enforced via `@PreAuthorize` role checks — patients, doctors, and admins each only see what they're allowed to.

---

## 🚀 Running It Locally

**Prerequisites:** Java 17, Maven (or use the included wrapper), PostgreSQL.

```bash
# 1. Clone the repo
git clone https://github.com/GanningJoel-05/Smart-Queue-Management-System.git
cd Smart-Queue-Management-System/queuemanager

# 2. Configure your database and secrets in
#    src/main/resources/application.properties
#    (datasource URL/username/password, jwt.secret, jwt.expiry.ms)

# 3. Run it
./mvnw spring-boot:run
```

The API will be live at `http://localhost:8080`.

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **WebSocket endpoint:** `ws://localhost:8080/ws-queue` (SockJS fallback supported)

---

## 🗺️ Roadmap Ideas

- Notification integration (SMS/push) alongside WebSocket events
- Multi-clinic doctor support
- Patient-facing dashboard UI
- Dockerized deployment with docker-compose (Postgres + app)

---

## 👤 Author

**Joel Ganning**
[GitHub](https://github.com/GanningJoel-05)

---

*This project was built to demonstrate practical backend engineering: real-time systems, role-based security, clean layered architecture, and solving an everyday operational problem with code.*
