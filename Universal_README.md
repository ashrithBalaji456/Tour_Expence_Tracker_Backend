<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0EA5E9,50:8B5CF6,100:EC4899&height=220&section=header&text=Tour%20Expense%20Tracker&fontSize=48&fontColor=ffffff&animation=fadeIn&fontAlignY=35&desc=Split.%20Track.%20Travel%20Stress-Free.&descAlignY=55&descSize=18" />

<a href="https://tour-expence-tracker-frontend.vercel.app/">
  <img src="https://readme-typing-svg.demolab.com?font=Fira+Code&weight=600&size=24&duration=3000&pause=800&color=0EA5E9&center=true&vCenter=true&width=650&lines=Group+Trip+Expense+Tracking%2C+Simplified;Auto-Split+Bills+%7C+Settle+Debts+Instantly;React+%2B+Vite+%2B+Spring+Boot+%2B+PostgreSQL;Built+for+Real+Trips%2C+Used+on+Real+Trips" />
</a>

<br/>

[![Live Demo](https://img.shields.io/badge/🚀_Live_Demo-Visit_App-0EA5E9?style=for-the-badge)](https://tour-expence-tracker-frontend.vercel.app/)
[![Frontend Repo](https://img.shields.io/badge/Frontend_Repo-GitHub-181717?style=for-the-badge&logo=github)](https://github.com/ashrithBalaji456/Tour_Expence_Tracker_Frontend)
[![Backend Repo](https://img.shields.io/badge/Backend_Repo-GitHub-181717?style=for-the-badge&logo=github)](https://github.com/ashrithBalaji456/Tour_Expence_Tracker_Backend)

![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react&logoColor=black)
![Vite](https://img.shields.io/badge/Vite-5.0-646CFF?style=flat-square&logo=vite&logoColor=white)
![TailwindCSS](https://img.shields.io/badge/TailwindCSS-3.4-38B2AC?style=flat-square&logo=tailwindcss&logoColor=white)
![Framer Motion](https://img.shields.io/badge/Framer_Motion-11-FF00C1?style=flat-square&logo=framer&logoColor=white)
![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Vercel](https://img.shields.io/badge/Deployed_on-Vercel-000000?style=flat-square&logo=vercel&logoColor=white)
![Render](https://img.shields.io/badge/Deployed_on-Render-46E3B7?style=flat-square&logo=render&logoColor=white)

</div>

---

## 📖 About the Project

**Tour Expense Tracker** is a full-stack web application built to solve a very real, very annoying problem — **splitting group trip expenses fairly without the WhatsApp-math chaos**.

It has two coordinated phases:
- **Pre-Trip Planning** — log upfront bookings (train tickets, hotel advances, deposits) and see a fair split before the trip even begins.
- **Active Trip Tracking** — log every daily purchase (food, transport, drinks, entertainment) in real time, and instantly see who owes whom, down to the rupee.

The entire debt web collapses into the **minimum number of settlements** needed to clear everyone's balance — no manual reconciliation required.

> 🧳 This app was built and actually used on a real group trip (Goa 2026) to track ₹30,000+ in shared spending across multiple members, categories, and payment modes.

---

## 🗂️ Repository Structure

| Repo | Description | Link |
|---|---|---|
| 🖥️ **Frontend** | React + Vite + TailwindCSS SPA, deployed on Vercel | [Tour_Expence_Tracker_Frontend](https://github.com/ashrithBalaji456/Tour_Expence_Tracker_Frontend) |
| ⚙️ **Backend** | Spring Boot + PostgreSQL REST API, deployed on Render | [Tour_Expence_Tracker_Backend](https://github.com/ashrithBalaji456/Tour_Expence_Tracker_Backend) |
| 🌐 **Live App** | Production deployment | [tour-expence-tracker-frontend.vercel.app](https://tour-expence-tracker-frontend.vercel.app/) |

---

## ✨ Feature Highlights

<table>
<tr>
<td width="50%" valign="top">

### 🎨 Frontend
- Glassmorphic dark dashboard UI
- Framer Motion micro-animations across screens & modals
- Recharts-powered analytics (donut + bar charts)
- Custom click-outside-aware dropdowns (dates, categories, sorting)
- Express 1-tap purchase logging
- Multi-group support with admin-only trip deletion

</td>
<td width="50%" valign="top">

### ⚙️ Backend
- Stateless JWT authentication (24h token expiry)
- Trip-group scope isolation via request headers
- Dual-phase split engine (pre-trip vs active spends)
- Self-healing DB constraint patches on startup
- Optimized P2P settlement algorithm (minimum transfers)
- Clean REST API with full CRUD on expenses & bookings

</td>
</tr>
</table>

---

## 🔄 End-to-End Application Workflow

```mermaid
flowchart TD
    A([👤 User Registers / Logs In]) --> B[🧭 Create Trip Group & Invite Members]
    B --> C[💰 Set Pre-Trip Budget & Log Bookings]
    C --> D[🧾 Log Daily Purchases During Trip]
    D --> E[📊 View Real-Time Split & Category Analytics]
    E --> F{Balances Even?}
    F -- No --> G[🤝 Trigger Peer-to-Peer Settlements]
    G --> E
    F -- Yes --> H([✅ Trip Fully Settled])
    H --> I[🗑️ Admin Archives / Deletes Group]

    style A fill:#0F172A,stroke:#0EA5E9,stroke-width:2px,color:#fff
    style B fill:#0F172A,stroke:#0EA5E9,stroke-width:2px,color:#fff
    style C fill:#0F172A,stroke:#8B5CF6,stroke-width:2px,color:#fff
    style D fill:#0F172A,stroke:#8B5CF6,stroke-width:2px,color:#fff
    style E fill:#0F172A,stroke:#EAB308,stroke-width:2px,color:#fff
    style F fill:#0F172A,stroke:#F43F5E,stroke-width:2px,color:#fff
    style G fill:#0F172A,stroke:#F43F5E,stroke-width:2px,color:#fff
    style H fill:#0F172A,stroke:#10B981,stroke-width:2px,color:#fff
    style I fill:#0F172A,stroke:#EF4444,stroke-width:2px,color:#fff
```

---

## 🔐 Auth & Session Sequence

```mermaid
sequenceDiagram
    autonumber
    actor User
    User->>Frontend: Enter credentials
    Frontend->>Backend: POST /api/auth/login
    Backend->>Database: Validate user credentials
    Database-->>Backend: User record
    Backend-->>Frontend: JWT token + group list
    Frontend->>Frontend: Store token & activeGroupId
    User->>Frontend: Select active trip group
    Frontend->>Backend: GET /api/dashboard/summary (X-Trip-Group-Id header)
    Backend->>Database: Aggregate expenses, budgets, settlements
    Database-->>Backend: Aggregated results
    Backend-->>Frontend: Dashboard payload
    Frontend-->>User: Render charts, balances & purchase list
```

---

## 🧮 Settlement Calculation Flow

```mermaid
flowchart LR
    A[All Logged Expenses] --> B[Group by Member Paid]
    B --> C[Compute Total Spent per Member]
    C --> D[Compare vs Equal Share / Person]
    D --> E{Balance}
    E -->|Positive| F[Creditor 🟢]
    E -->|Negative| G[Debtor 🔴]
    F --> H[Minimum Transfer Matching Engine]
    G --> H
    H --> I[Final: Who Owes Whom List]

    style A fill:#0F172A,stroke:#8B5CF6,color:#fff
    style H fill:#0F172A,stroke:#0EA5E9,color:#fff
    style I fill:#0F172A,stroke:#10B981,color:#fff
```

---

## 🗄️ Database Entity Relationship Diagram

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

## 🧩 Frontend Component Architecture

```mermaid
graph TD
    App[App.jsx] --> AuthScreens[AuthScreens.jsx]
    App --> MainDashboard[Main Dashboard View]

    MainDashboard --> Header[Header / Group Selector / Sign Out]
    MainDashboard --> QuickActions[Quick 1-Tap Purchase Buttons]
    MainDashboard --> SummaryCards[Summary Cards: Spent, Budget, Remaining]
    MainDashboard --> SplitSettlements[Trip Settlements / Budget Status]
    MainDashboard --> AnalyticsCharts[Analytics: Pie & Bar Graphs]
    MainDashboard --> CalculatorWidget[Calculator Widget]
    MainDashboard --> ExpenseList[Daily Purchases List]
    MainDashboard --> PreTripPlanner[Pre-Trip Planner / Bookings View]

    ExpenseList --> CustomDatePicker[CustomDatePicker.jsx]
    ExpenseList --> CategoryDropdown[Custom Category Menu]
    ExpenseList --> SortDropdown[Custom Sort Menu]

    style App fill:#0F172A,stroke:#0EA5E9,stroke-width:2px,color:#fff
    style AuthScreens fill:#0F172A,stroke:#8B5CF6,stroke-width:1px,color:#fff
    style MainDashboard fill:#0F172A,stroke:#10B981,stroke-width:1px,color:#fff
    style PreTripPlanner fill:#0F172A,stroke:#EAB308,stroke-width:1px,color:#fff
```

---

## 📊 Category Spend Distribution (Sample Trip Data)

```mermaid
pie showData
    title Category-wise Spending Share
    "Food" : 38
    "Transport" : 34
    "Entertainment" : 22
    "Drinks" : 6
```

---

## 🛠️ Tech Stack

<div align="center">

| Layer | Technology |
|---|---|
| Frontend Framework | React 18 + Vite 5 |
| Styling | TailwindCSS 3.4 |
| Animation | Framer Motion 11 |
| Charts | Recharts |
| Backend Framework | Spring Boot 3.2.5 (Java 17) |
| Database | PostgreSQL 15 |
| Auth | Stateless JWT |
| Build Tool | Maven |
| Frontend Hosting | Vercel |
| Backend Hosting | Render (Docker) |

</div>

---

## 🔌 Core API Endpoints

| Category | Method | Endpoint | Description |
|---|---|---|---|
| Auth | `POST` | `/api/auth/register` | Register a new user |
| Auth | `POST` | `/api/auth/login` | Login & retrieve JWT |
| Group | `POST` | `/api/auth/group` | Create trip group & invite members |
| Group | `GET` | `/api/auth/groups` | List groups the user belongs to |
| Group | `DELETE` | `/api/auth/group/{id}` | Delete a trip group (admin only) |
| Dashboard | `GET` | `/api/dashboard/summary` | Combined budget, spend & settlement data |
| Expenses | `GET / POST / PUT / DELETE` | `/api/expenses` | Full CRUD on daily purchases |
| Pre-Trip | `GET / POST` | `/api/pretrip/members` | Manage pre-trip budgets |
| Pre-Trip | `GET` | `/api/pretrip/summary` | Pre-trip split analysis |
| Funds | `GET / POST` | `/api/funds` | Cash pool contributions |

---

## 🚀 Quick Start

```bash
# 1. Clone both repositories
git clone https://github.com/ashrithBalaji456/Tour_Expence_Tracker_Frontend.git
git clone https://github.com/ashrithBalaji456/Tour_Expence_Tracker_Backend.git

# 2. Start the backend (Java 17 + Maven + PostgreSQL required)
cd Tour_Expence_Tracker_Backend
mvn spring-boot:run

# 3. Start the frontend (in a separate terminal)
cd Tour_Expence_Tracker_Frontend
npm install
npm run dev
```

Full setup instructions live in each repo's dedicated README (linked above).

---

## 🌐 Try It Live

<div align="center">

[![Open App](https://img.shields.io/badge/Open_Live_App-tour--expence--tracker-0EA5E9?style=for-the-badge&logo=vercel)](https://tour-expence-tracker-frontend.vercel.app/)

⚠️ *Backend is hosted on Render's free tier — the first request may take 30–60s to spin up the server from a cold start.*

</div>

---

## 🗺️ Roadmap

- [ ] Multi-currency support
- [ ] Expense receipt image uploads
- [ ] Push notifications for settlement reminders
- [ ] Faster free-tier backend hosting (Railway / Fly.io evaluation)
- [ ] Export trip summary as PDF

---

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:EC4899,50:8B5CF6,100:0EA5E9&height=120&section=footer" />

Made with ☕, ₹ math, and one very persistent group chat.

</div>
