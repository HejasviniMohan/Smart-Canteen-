# Smart Canteen Management System

A full-stack smart campus canteen management system designed to help students
**order food in advance, avoid queues, make payments, receive a digital pickup
token, and collect food using a QR code.**

The system provides separate interfaces for students, canteen staff, and
administrators to manage the complete food ordering and pickup workflow.

---

## 🎯 Problem Statement

Traditional college canteens often have long queues during lunch and break
hours. Students spend a significant amount of time waiting to place orders
and collect food.

The Smart Canteen Management System solves this problem by allowing students
to:

1. Browse the menu online
2. Select food items
3. Add items to the cart
4. Place an order in advance
5. Select a payment method
6. Receive a digital order token
7. Receive a QR code for pickup
8. Track order preparation
9. Show the QR code at the canteen
10. Collect the prepared food without waiting in a long queue

---

## 🚀 Key Features

### 👨‍🎓 Student

- User registration and login
- JWT-based authentication
- Browse food menu
- Search food items
- Browse food by categories
- Today's Specials
- Food availability status
- Add/remove items from cart
- Update item quantities
- Checkout
- Multiple payment methods
- Digital order token generation
- QR code generation
- Order confirmation
- Live order status tracking
- Order history
- Order details
- Reviews and ratings
- Profile management
- Notifications

### Order Flow

```
Browse Menu → Select Food → Add to Cart → Checkout → Select Payment Method
→ Payment / Confirmation → Order Created → Digital Token Generated
→ QR Code Generated → Canteen Prepares Order → Student Shows QR Code
→ Staff Verifies QR → Food Collected
```

---

### 👨‍🍳 Canteen Staff

Staff members can manage day-to-day canteen operations.

- Staff login
- View incoming orders
- View order details
- Update order status
- Accept orders
- Mark orders as preparing
- Mark orders as ready
- Verify digital tokens
- Scan/verify customer QR codes
- Mark orders as collected
- Handle Cash on Pickup payments
- Toggle food availability
- Monitor active orders

### Order Status

```
PLACED → ACCEPTED → PREPARING → READY → COMPLETED
```

---

### 👨‍💼 Admin

Administrators have complete control over the canteen system.

- Admin authentication
- User management (students and staff)
- Food management (add/edit/delete)
- Category management
- Food availability management
- Today's Specials management
- Order management
- Payment monitoring
- Sales and order analytics
- Dashboard statistics
- Review management

---

## 💳 Payment System

The application supports multiple payment options:

- **UPI** — Demo/sandbox mode
- **Card** — Demo/sandbox mode with form validation
- **Wallet** — Demo/sandbox mode
- **Cash on Pickup** — Pay in person when collecting

> ⚠️ **Demo/Sandbox Payment** — All payment methods use a simulated flow for
> demonstration purposes. No real money is transferred. Production deployments
> must configure real payment gateway credentials.

### Payment Flow (Online Methods)

```
Select Payment Method → Validate Payment → Payment Successful
→ Create Order → Generate Token → Generate QR Code
```

### Payment Flow (Cash on Pickup)

```
Select Cash on Pickup → Place Order → Payment Status = PENDING
→ Generate Token + QR → Collect Food → Staff Receives Cash
→ Staff Marks Payment PAID → Order Complete
```

---

## 🎟️ Digital Token System

Every successful order receives a unique digital pickup token (e.g. `A100`).

The token helps canteen staff quickly identify and process customer orders
without requiring the customer to stand in a queue.

---

## 📱 QR Pickup System

A QR code is generated for every confirmed order. The student can access the
QR code from:

- Order Confirmation page
- My Orders page
- Order Details page

### Pickup Process

```
Student places order → Digital Token Generated → QR Code Generated
→ Canteen prepares food → Student reaches pickup counter
→ Student shows QR Code → Staff verifies QR → Order marked COLLECTED
→ Student receives food
```

The QR code contains only an order/token reference — **no sensitive payment
information**.

---

## 🖥️ Technology Stack

### Frontend
- React.js 19
- Vite
- JavaScript
- Bootstrap 5
- Framer Motion
- Recharts

### Backend
- Java 17
- Spring Boot 3.3.5
- Spring Security + JWT
- BCrypt password hashing
- Spring Data JPA / Hibernate
- REST APIs
- Swagger / OpenAPI

### Database
- **Dev:** H2 (in-memory, PostgreSQL compatibility mode)
- **Prod:** PostgreSQL

### Other
- **QR Code:** ZXing
- **Payment:** Demo/Sandbox flow (Razorpay-compatible structure)
- **Deployment:** Render (backend), Vercel (frontend)

---

## 🏗️ Project Architecture

```
                 Smart Canteen
                      │
          ┌───────────┴───────────┐
          │                       │
     React Frontend       Spring Boot Backend
          │                       │
          │                  REST APIs + JWT
          │                       │
          │                 Service Layer
          │                       │
          │                 Repository Layer
          │                       │
          └───────────────┬───────┘
                          │
                    PostgreSQL
```

---

## 📁 Project Structure

```
smart-canteen/
│
├── backend/
│   ├── src/main/java/com/canteen/
│   │   ├── config/       # Security, CORS, OpenAPI, DataSeeder
│   │   ├── controller/   # REST endpoints
│   │   ├── dto/          # Request/Response DTOs
│   │   ├── entity/       # JPA entities
│   │   ├── exception/    # Error handling
│   │   ├── repository/   # JPA repositories
│   │   ├── security/     # JWT filter, UserDetailsService
│   │   ├── service/      # Business logic
│   │   └── util/         # TokenGenerator
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   ├── application-dev.properties
│   │   └── application-prod.properties
│   ├── src/test/
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── components/   # Reusable UI components
│   │   ├── context/      # Auth + Cart React contexts
│   │   ├── data/         # Static data
│   │   ├── pages/        # Route pages
│   │   ├── services/     # API clients
│   │   └── styles.css    # Global styles
│   ├── public/
│   ├── package.json
│   └── vite.config.js
│
├── db/
│   └── schema.sql        # PostgreSQL production schema
│
├── docker-compose.yml
├── README.md
└── .gitignore
```

---

## 🔐 Authentication & Authorization

Three roles are supported:

| Role    | Description                                  |
|---------|----------------------------------------------|
| STUDENT | Browse, order, pay, track, review            |
| STAFF   | Manage orders, update status, verify QR, toggle availability |
| ADMIN   | Full system access: users, foods, categories, analytics |

---

## ⚙️ Local Development Setup

### Prerequisites

- JDK 17+
- Maven 3.9+
- Node.js 18+
- npm

---

### Backend Setup

```bash
cd backend
mvn spring-boot:run
```

Backend available at: `http://localhost:8080`

API docs (Swagger): `http://localhost:8080/swagger-ui/index.html`

H2 Console: `http://localhost:8080/h2-console`

---

### Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

Frontend available at: `http://localhost:5173`

---

### Running Both Simultaneously

```bash
# Terminal 1 — Backend
cd backend && mvn spring-boot:run

# Terminal 2 — Frontend
cd frontend && npm install && npm run dev
```

Open `http://localhost:5173` in your browser.

---

## 👤 Demo Accounts

> ⚠️ **For local development/demo only. Never use in production.**

| Role    | Email                        | Password    |
|---------|------------------------------|-------------|
| Admin   | admin@canteen.com            | Admin@123   |
| Staff   | staff@canteen.com           | Staff@123   |
| Student | student@canteen.com         | Student@123 |

---

## 🧪 Testing Checklist

### Student
- [ ] Register / Login
- [ ] Browse menu and search
- [ ] Filter by category
- [ ] View Today's Specials
- [ ] Add/update/remove cart items
- [ ] Checkout with UPI / Card / Wallet
- [ ] Checkout with Cash on Pickup
- [ ] Receive digital token
- [ ] View and save QR code
- [ ] Track order status
- [ ] View order history
- [ ] Submit a review

### Staff
- [ ] Staff login
- [ ] View active orders
- [ ] Accept / Prepare / Mark Ready
- [ ] Verify QR / token
- [ ] Handle Cash on Pickup (mark as PAID)
- [ ] Mark order as collected
- [ ] Toggle food availability

### Admin
- [ ] Admin login
- [ ] Manage users
- [ ] Manage food items
- [ ] Manage categories
- [ ] Mark Today's Specials
- [ ] Toggle food availability
- [ ] View all orders
- [ ] View analytics dashboard

---

## 🔒 Security Notes

Production deployments must configure:

- `JWT_SECRET` — strong, unique secret key
- `POSTGRES_PASSWORD` — database password
- `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` — real gateway credentials
- `CORS_ALLOWED_ORIGINS` — production frontend URL

**Never commit secrets, passwords, or API keys to GitHub.**

---

## 🌐 Production Deployment (Planned)

```
         Users
           │
           ▼
   Vercel Frontend
           │
           ▼
  Render Spring Boot API
           │
           ▼
     PostgreSQL
```

Before going live: configure all environment variables, enable HTTPS,
change all demo credentials, and run full end-to-end tests.

---

## 🎯 Project Objective

Reduce canteen waiting times by moving ordering and payment online.

**Traditional canteen:**
```
Student → Queue → Place Order → Wait → Collect Food
```

**Smart Canteen:**
```
Student → Order Online → Pay/Confirm → Get Token + QR
→ Canteen Prepares → Show QR → Collect Food
```

**Less waiting. Faster pickup. Better management.**
