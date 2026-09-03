# Resource Booking API

A RESTful backend service built with Spring Boot, Spring Security, JWT, JPA, and MySQL/PostgreSQL that manages bookable resources and user reservations with role-based access control (RBAC), double-booking prevention, dynamic search/filtering, and automatic database seeding.

---

## Features

- **JWT Authentication & RBAC**:
  - `POST /auth/register` and `POST /auth/login` for user authentication.
  - Role-based authorization (`USER` vs `ADMIN`) using Spring Security method-level security (`@PreAuthorize`).
  - Passwords securely hashed with BCrypt.
- **Resource Management**:
  - `ADMIN`: Full CRUD capabilities over bookable resources (Rooms, Vehicles, Equipment).
  - `USER`: Read-only access to view and search available resources.
- **Reservation Lifecycle & Double-Booking Protection**:
  - Users create bookings taken directly from their JWT credentials (no identity spoofing).
  - Overlap detection query (`startTime < existing.endTime AND endTime > existing.startTime`) rejects overlapping reservations on the same resource with `409 Conflict`.
  - Normal users can view and cancel only their own reservations.
  - Admins can view all reservations, update status directly, and delete records.
- **Filtering, Pagination & Sorting**:
  - Filter reservations dynamically using JPA `Specification` by status, minimum price, and maximum price.
  - Supports `page`, `size`, `sortBy`, and `sortDir` parameters.
- **Automated Startup Data Seeder**:
  - Automatically seeds default users (`admin/admin123`, `user/user123`) and sample resources on application startup.

---

## Tech Stack

- **Java 17+**
- **Spring Boot 3.x / 4.x** (Spring Web, Spring Security, Spring Data JPA, Validation)
- **Database**: MySQL / PostgreSQL (H2 in-memory used for test profile)
- **Authentication**: Stateless JWT (`io.jsonwebtoken:jjwt-api:0.12.6`)
- **Testing**: JUnit 5, Mockito, Spring Boot Test, MockMvc

---

## Prerequisites

- **Java 17** or higher installed and configured in your `PATH`
- **Maven 3.8+** (or use the included `./mvnw` wrapper)
- **MySQL** instance running locally (or configured via environment variables)

---

## Configuration & Environment Variables

The application can be configured via environment variables or directly inside `src/main/resources/application.properties`:

| Variable | Default Value | Description |
|:---|:---|:---|
| `PORT` | `8080` | HTTP Server port |
| `DB_URL` | `jdbc:mysql://localhost:3306/resource_booking_db?...` | JDBC Database Connection URL |
| `DB_USERNAME` | `root` | Database Username |
| `DB_PASSWORD` | `admin` | Database Password |
| `JWT_SECRET` | `ReplaceThisWithALongRandomBase64SecretKey...` | Secret key used for signing JWTs (min 256 bits) |
| `JWT_EXPIRATION_MS` | `86400000` (24 hours) | Token expiration duration in milliseconds |

---

## Running the Application

### 1. Clone the Repository
```bash
git clone https://github.com/exelynt-learning-platform/backend-developer-as-final-72056-raviraju.git
cd backend-developer-as-final-72056-raviraju
```

### 2. Build the Project
```bash
./mvnw clean package -DskipTests
```
*(On Windows PowerShell, use `.\mvnw.cmd clean package -DskipTests`)*

### 3. Run the Application
```bash
./mvnw spring-boot:run
```

The API will start at: `http://localhost:8080`

---

## Running Automated Tests

Run the complete test suite including security tests, controller endpoint tests, and conflict validation service tests:

```bash
./mvnw test
```
*(On Windows PowerShell, use `.\mvnw.cmd test`)*

---

## Default Seeded Accounts

The application automatically provisions the following accounts on first startup:

| Username | Password | Role | Description |
|:---|:---|:---|:---|
| `admin` | `admin123` | `ROLE_ADMIN` | Full CRUD permissions on resources and all reservations |
| `user` | `user123` | `ROLE_USER` | Can browse resources and create/manage own reservations |

---

## REST API Endpoints Overview

### 1. Authentication Endpoints (`/auth`)
- `POST /auth/register` - Register a new user account (`USER` by default)
- `POST /auth/login` - Authenticate with username and password, returns JWT token

### 2. Resource Endpoints (`/api/resources`)
- `GET /api/resources` - List resources with optional filters (`?type=ROOM&available=true`) *(User / Admin)*
- `GET /api/resources/{id}` - Get resource details by ID *(User / Admin)*
- `POST /api/resources` - Create a new resource *(Admin only)*
- `PUT /api/resources/{id}` - Update existing resource *(Admin only)*
- `DELETE /api/resources/{id}` - Delete resource *(Admin only)*

### 3. Reservation Endpoints (`/api/reservations`)
- `POST /api/reservations` - Create a reservation (Double-booking protected) *(User / Admin)*
- `GET /api/reservations` - List reservations with pagination & filtering (`?status=CONFIRMED&minPrice=50&maxPrice=500&page=0&size=10`) *(Users see own, Admin sees all)*
- `GET /api/reservations/{id}` - Get reservation by ID *(Owner or Admin)*
- `PATCH /api/reservations/{id}/cancel` - Cancel a reservation *(Owner or Admin)*
- `PATCH /api/reservations/{id}/status` - Update reservation status directly *(Admin only)*
- `DELETE /api/reservations/{id}` - Delete reservation record *(Admin only)*
