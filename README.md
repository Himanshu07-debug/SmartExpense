# ⚡ SmartExpense — Distributed FinTech Microservices Engine

An enterprise-grade, event-driven financial intelligence and ledger platform. Built with **Java 21**, **Spring Boot 3**, **Apache Kafka**, **Spring Cloud Gateway WebMVC**, **FastAPI (Google Gemini 3.6 Flash)**, and **ReactJS**.

The platform automates financial SMS telemetry extraction via generative AI, enforces edge-level authentication, and synchronizes transactions across isolated domain schemas using asynchronous, idempotent event messaging.

---

## 🏗️ Architecture Topology

```
+--------------------------+
                     |  Browser / Client :3000  |
                     +--------------------------+
                                  |
                                  | HTTP / REST + Bearer JWT
                                  v
             +------------------------------------------+
             |      API Gateway (Spring Boot :9000)     |
             |  • Request Body Stream Caching           |
             |  • Non-blocking JWT Ingress Filter       |
             |  • Preflight CORS Security Handshake     |
             +------------------------------------------+
                   |              |              |
         +---------+              |              +---------+
         |                        |                        |
         v                        v                        v
+------------------+     +------------------+     +------------------+
|   Auth Service   |     |   User Service   |     |    DS Service    |
|   (Port :8080)   |     |   (Port :8081)   |     |  (FastAPI :8010) |
| • JWT / BCrypt   |     | • Profile Mgmt   |     | • Regex Gate     |
| • Refresh Token  |     | • MySQL Sync     |     | • Gemini 3.6     |
+------------------+     +------------------+     +------------------+
         |                        ^                        |
         | (USER_EVENTS)          |                        | (EXPENSE_EVENTS)
         +------------------------+                        |
                                                           v
                                                  +------------------+
                                                  | Expense Service  |
                                                  |   (Port :8082)   |
                                                  | • Core Ledger    |
                                                  | • MySQL Persist  |
                                                  +------------------+

```

---

## 🚀 Key Architectural Highlights

- **Polyglot Microservices:** Enterprise Java 21 Spring Boot core combined with a high-throughput Python FastAPI service dedicated to AI execution.
- **Edge Security & Payload Caching:** Spring Cloud Gateway WebMVC validates HMAC-SHA256 JWT claims, propagates identity context headers (`X-User-Id`), and uses `CachedBodyHttpServletRequest` to prevent downstream request body consumption issues.
- **Eventual Consistency via Kafka:** Decoupled messaging over Kafka topics (`USER_EVENTS`, `EXPENSE_EVENTS`) isolates database boundaries and removes distributed transaction bottlenecks.
- **Sub-800ms Unstructured SMS Parsing:** Combines an efficient regex financial filter with Google Gemini 3.6 Flash to extract structured transaction data from raw bank SMS strings.
- **Live Telemetry & Ledger Console:** A single-page React command center tracking live execution traces, JWT session validity, end-to-end latency, and real-time database updates.

---

## 📦 Services Breakdown

| Service | Port | Tech Stack | Primary Responsibility |
| :--- | :--- | :--- | :--- |
| **`api-gateway`** | `9000` | Spring Cloud Gateway WebMVC, JWT | Ingress routing, body caching, security filtering, CORS management |
| **`auth-service`** | `8080` | Spring Boot 3, Spring Security 6, Flyway | Credential validation, JWT & Refresh token lifecycle, `USER_EVENTS` publishing |
| **`user-service`** | `8081` | Spring Boot 3, Spring Data JPA, MySQL | User profile management, consumer for `USER_EVENTS` |
| **`expense-service`**| `8082` | Spring Boot 3, Spring Data JPA, MySQL | Financial ledger operations, consumer for `EXPENSE_EVENTS` |
| **`ds-service`** | `8010` | FastAPI, Google Gemini 3.6 Flash, Kafka | SMS filtering, AI entity extraction, `EXPENSE_EVENTS` producer |
| **`expense-ui`** | `3000` | React, Lucide Icons, Fetch API | Single-page command center, telemetry monitor, live ledger view |

---

## 🛠️ Infrastructure & Local Setup

### Prerequisites
- Java 21 LTS
- Python 3.10+
- Node.js 18+
- Docker & Docker Compose
- MySQL 8.0+

### 1. Infrastructure Bootstrapping (Kafka & Zookeeper)
```bash
docker-compose up -d

```

### 2. Microservices Execution

Start each microservice from its corresponding project directory:

```bash
# 1. Auth Service
cd auth-service && ./mvnw spring-boot:run

# 2. User Service
cd user-service && ./mvnw spring-boot:run

# 3. Expense Service
cd expense-service && ./mvnw spring-boot:run

# 4. Data Science AI Service
cd ds-service
source venv/bin/activate
pip install -r requirements.txt
uvicorn app:app --port 8010 --http h11 --reload

# 5. API Gateway
cd api-gateway && ./mvnw spring-boot:run

# 6. React Frontend
cd expense-ui
npm install
npm start

```

---

## 🧪 End-to-End Verification Flow

1. Open the UI Console at `http://localhost:3000`.
2. Register a new user via **Sign Up** (`POST /auth/v1/signup`) — triggers asynchronous user profile provisioning in `user_service_db` via Kafka (`USER_EVENTS`).
3. Sign in via **Sign In** (`POST /auth/v1/login`) — retrieves a Bearer JWT; synchronizes the numeric `userId` directly with the active UI session state.
4. Select or paste a financial transaction SMS into the simulator and trigger ingestion.
5. Watch the real-time event pipeline trace:
* **Gateway (:9000)** ➔ **DS Service (:8010)** ➔ **Gemini 3.6 Flash Extraction** ➔ **Kafka Topic (`EXPENSE_EVENTS`)** ➔ **Expense Service (:8082)** ➔ **MySQL Database Persistence**.



---

## 📄 License & Copyright

Copyright © 2026 Himanshu Sharma. All rights reserved.

This project is licensed under the [MIT License](https://www.google.com/search?q=LICENSE&utm_source=gemini) — free for educational, personal, and commercial software portfolio demonstrations.