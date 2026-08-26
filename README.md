# Smart Canteen Management System

A placement-quality full-stack web application for managing canteen operations:
ordering, payments, digital tokens, live tracking, reviews, and admin analytics.

## Stack

- **Frontend:** React.js (Vite), JavaScript, Bootstrap, Recharts
- **Backend:** Java 17, Spring Boot, Spring Security, JWT, JPA/Hibernate
- **Database (dev):** H2 in PostgreSQL-compatibility mode
- **Database (prod):** PostgreSQL
- **API:** REST + Swagger/OpenAPI
- **Auth:** JWT (HS256), BCrypt
- **Payment:** Razorpay Sandbox
- **QR:** ZXing (backend), scanned on mobile
- **Deployment:** Render (backend + Postgres), Vercel (frontend)

## Monorepo Layout

```
smart-canteen/
├── backend/     Spring Boot application
├── frontend/    React.js (Vite) application
├── db/          PostgreSQL schema & seed scripts (for production)
└── README.md
```

## Roles

- **STUDENT** – browse menu, place orders, pay, track, review
- **STAFF**   – view incoming orders, update status, toggle availability
- **ADMIN**   – manage users, foods, categories, orders, view analytics

## Quick Start (Local Dev)

### Prerequisites
- JDK 17+
- Maven 3.9+
- Node.js 18+

### Backend
```bash
cd backend
mvn spring-boot:run
# API at http://localhost:8080
# Swagger UI at http://localhost:8080/swagger-ui.html
# H2 console at http://localhost:8080/h2-console
```

### Frontend
```bash
cd frontend
npm install
npm run dev
# UI at http://localhost:5173
```

### Default Credentials (seeded)
- **Admin:**  admin@canteen.com  /  Admin@123
- **Staff:**  staff@canteen.com  /  Staff@123
- **Student:** student@canteen.com / Student@123

## Documentation

See [`docs/`](./docs) for full architecture, API design, DB schema, and deployment guide.