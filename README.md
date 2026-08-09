<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:6DB33F,100:0EA5E9&height=200&section=header&text=Tour%20Expense%20Tracker&fontSize=42&fontColor=ffffff&animation=fadeIn&fontAlignY=35&desc=Backend%20%E2%80%94%20Spring%20Boot%20%2B%20PostgreSQL&descAlignY=55&descSize=16" />

<img src="https://readme-typing-svg.demolab.com?font=Fira+Code&weight=600&size=22&duration=3000&pause=800&color=6DB33F&center=true&vCenter=true&width=600&lines=Stateless+JWT+Authentication;Optimized+P2P+Settlement+Engine;Dual-Phase+Split+Tracking+System" />

[![Java Version](https://img.shields.io/badge/Java-17-orange.svg?style=for-the-badge&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-brightgreen.svg?style=for-the-badge&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg?style=for-the-badge&logo=postgresql)](https://www.postgresql.org/)
[![Maven](https://img.shields.io/badge/Maven-3.8-red.svg?style=for-the-badge&logo=apachemaven)](https://maven.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg?style=for-the-badge&logo=docker)](https://www.docker.com/)

[![Live App](https://img.shields.io/badge/🚀_Live_App-Visit-0EA5E9?style=for-the-badge)](https://tour-expence-tracker-frontend.vercel.app/)
[![Frontend Repo](https://img.shields.io/badge/Frontend_Repo-GitHub-181717?style=for-the-badge&logo=github)](https://github.com/ashrithBalaji456/Tour_Expence_Tracker_Frontend)

</div>

---

## ✈️ About

This repository contains the backend REST API engine for the **Tour Expense Tracker** application. It handles user authentication, group expense sharing, pre-trip planning logistics, real-time peer-to-peer debt settlements, and group fund deposits.

🔗 **Live Application:** [tour-expence-tracker-frontend.vercel.app](https://tour-expence-tracker-frontend.vercel.app/)
🔗 **Frontend Repo:** [Tour_Expence_Tracker_Frontend](https://github.com/ashrithBalaji456/Tour_Expence_Tracker_Frontend)

---

## 🚀 Architectural & User Workflow

```mermaid
flowchart TD
    A[User Registration / Login] --> B[Create Trip Group & Invite Members]
    B --> C[Set Up Pre-Trip Budget & Log Bookings]
    C --> D[Log Active Daily Purchases]
    D --> E[Check Real-Time Split Breakdown]
    E --> F[Trigger Peer-to-Peer Settlements]
    F --> G[All Balances Cleared]
    G --> H[Admin Deletes Trip Group & Dependencies]

    style A fill:#0F172A,stroke:#0EA5E9,stroke-width:2px,color:#fff
    style B fill:#0F172A,stroke:#0EA5E9,stroke-width:2px,color:#fff
    style C fill:#0F172A,stroke:#8B5CF6,stroke-width:2px,color:#fff
    style D fill:#0F172A,stroke:#8B5CF6,stroke-width:2px,color:#fff
    style E fill:#0F172A,stroke:#EAB308,stroke-width:2px,color:#fff
    style F fill:#0F172A,stroke:#F43F5E,stroke-width:2px,color:#fff
    style G fill:#0F172A,stroke:#10B981,stroke-width:2px,color:#fff
    style H fill:#0F172A,stroke:#EF4444,stroke-width:2px,color:#fff
```

---

## 🛠️ Feature Highlights

- **Secure JWT Authentication** — stateless session security with custom filters, 24-hour token duration
- **Trip Group Scope Isolation** — multi-user invitations; custom filters auto-detect group context from request headers to partition expenses cleanly
- **Dual-Phase Split Tracking**
  - *Pre-Trip Planning*: bookings like flights/hotels calculated separately against upfront budget caps
  - *Active Group Spends*: shared purchase splits, cash pool deposits, peer settlements
- **Self-Healing Constraint Schema** — custom SQL patches automatically execute on startup to self-heal constraint validations
- **P2P Settlement Algorithm** — optimized grid matching engine that computes the minimum number of cash transfers required to settle the group

---

## 🧮 Settlement Algorithm Logic

```mermaid
flowchart LR
    A[Fetch All Group Expenses] --> B[Aggregate Total Spent per Member]
    B --> C[Calculate Equal Share / Person]
    C --> D[Derive Net Balance per Member]
    D --> E{Net Balance}
    E -->|"> 0"| F[Creditor]
    E -->|"< 0"| G[Debtor]
    F --> H[Greedy Minimum-Transfer Matcher]
    G --> H
    H --> I[Return Optimal Settlement List]

    style A fill:#0F172A,stroke:#8B5CF6,color:#fff
    style D fill:#0F172A,stroke:#EAB308,color:#fff
    style H fill:#0F172A,stroke:#0EA5E9,color:#fff
    style I fill:#0F172A,stroke:#10B981,color:#fff
```

---

## 🗄️ Database Entity Schema (PostgreSQL)

```mermaid
erDiagram
    users {
        BIGINT id PK
        VARCHAR username UK
        VARCHAR email UK
        VARCHAR password
    }
    trip_groups {
        BIGINT id PK
        VARCHAR name
        TIMESTAMP created_at
        BIGINT creator_id FK
    }
    trip_group_members {
        BIGINT group_id FK
        VARCHAR username
    }
    expenses {
        BIGINT id PK
        VARCHAR title
        DECIMAL amount
        VARCHAR paid_by
        VARCHAR category
        VARCHAR payment_mode
        VARCHAR notes
        DATE expense_date
        BIGINT trip_group_id FK
    }
    pre_trip_members {
        BIGINT id PK
        VARCHAR name
        DECIMAL budget_limit
        BIGINT trip_group_id FK
    }
    pre_trip_expenses {
        BIGINT id PK
        VARCHAR title
        DECIMAL amount
        VARCHAR spent_by
        DATE expense_date
        VARCHAR notes
        BIGINT trip_group_id FK
    }
    fund_contributions {
        BIGINT id PK
        VARCHAR contributor_name
        DECIMAL amount
        DATE contribution_date
        VARCHAR payment_mode
        VARCHAR notes
        BIGINT trip_group_id FK
    }

    users ||--o{ trip_groups : "creates"
    trip_groups ||--|{ trip_group_members : "has"
    trip_groups ||--o{ expenses : "contains"
    trip_groups ||--o{ pre_trip_members : "manages"
    trip_groups ||--o{ pre_trip_expenses : "plans"
    trip_groups ||--o{ fund_contributions : "deposits"
```

---

## 🔌 API Endpoints

| Category | HTTP Method | Endpoint | Authorization | Description |
| :--- | :--- | :--- | :--- | :--- |
| **Auth** | `POST` | `/api/auth/register` | PermitAll | Register a new user account |
| **Auth** | `POST` | `/api/auth/login` | PermitAll | Login user & retrieve JWT bearer token |
| **Group** | `POST` | `/api/auth/group` | Authenticated | Create a new trip group & invite members |
| **Group** | `GET` | `/api/auth/groups` | Authenticated | Retrieve all trip groups the user is part of |
| **Group** | `DELETE` | `/api/auth/group/{id}` | Authenticated (Admin) | Delete a trip group and clear all related data |
| **Dashboard** | `GET` | `/api/dashboard/summary` | Authenticated | Retrieve combined budget, spends, splits & cash settlements |
| **Expenses** | `GET` | `/api/expenses` | Authenticated | Fetch active purchases in the group |
| **Expenses** | `POST` | `/api/expenses` | Authenticated | Log a new shared purchase |
| **Expenses** | `PUT` | `/api/expenses/{id}` | Authenticated | Edit details of a logged purchase |
| **Expenses** | `DELETE` | `/api/expenses/{id}` | Authenticated | Delete a purchase record |
| **Pre-Trip** | `GET` | `/api/pretrip/members` | Authenticated | Get budget caps and members planned |
| **Pre-Trip** | `POST` | `/api/pretrip/members` | Authenticated | Add a member or update budget cap |
| **Pre-Trip** | `GET` | `/api/pretrip/summary` | Authenticated | Get split analysis for pre-trip bookings |
| **Funds** | `GET` | `/api/funds` | Authenticated | Fetch all logged cash pool deposits |
| **Funds** | `POST` | `/api/funds` | Authenticated | Record a new contribution to the pool |

---

## 🛠️ Installation & Setup (Local)

### Prerequisites
- Java Development Kit (JDK 17+)
- Apache Maven 3.8+
- PostgreSQL Database instance

### Step 1: Clone Repository
```bash
git clone https://github.com/ashrithBalaji456/Tour_Expence_Tracker_Backend.git
cd Tour_Expence_Tracker_Backend
```

### Step 2: Configure Database Settings
Modify `src/main/resources/application.properties` with your PostgreSQL database credentials:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/expense_tracker
spring.datasource.username=your_username
spring.datasource.password=your_password

# JWT Config
jwt.expiration=86400000
```

### Step 3: Build and Run Application
```bash
mvn clean compile
mvn spring-boot:run
```
The server will boot up and listen for requests on `http://localhost:8080`.

---

## 🏗️ Production Deployment (Render / Docker)

The backend includes a configured production multi-stage Dockerfile for automated builds:

```dockerfile
# Build stage
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

To build and run as a Docker container:
```bash
docker build -t tour-expense-backend .
docker run -p 8080:8080 tour-expense-backend
```

> ⚠️ On Render's free tier, the service spins down after inactivity — expect a 30–60s cold start on the first request after idle time.

---

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0EA5E9,100:6DB33F&height=100&section=footer" />

</div>
