# User Story – Enriched Template

## # User Story Description

This vertical slice handles the core authentication flow, allowing active users to log in securely using their username and password to access their role-specific dashboard.

---

## # Story (INVEST)

**As** a User (Global Admin, Unit Admin, Teacher, or Auditor)  
**I want** to authenticate with my username and password  
**So that** I can access my role-specific dashboard

---

## # Analysis

### Approach

We will implement a standard JWT-based authentication flow. The `AuthService` will verify credentials against the database using `UserRepository`.
We are assuming the database and user tables already exist or will be covered in a prior DB script.

---

## # Starting Point

| | |
| -- | -- |
| **Project** | `atlas-backend` |
| **Classes** | `AuthController`, `AuthService`, `UserRepository`, `JwtService` |

---

## # Context

Refer to [raw-requirement.md](./raw-requirement.md) and pending questions in [open-questions.md](./open-questions.md).

---

## # Acceptance Criteria

**Scenario 1: Successful Login (Happy Path)**
* **Given** an existing active user account with a valid username and password
* **When** the user submits their credentials
* **Then** the system authenticates the user
* **And** returns a successful response (e.g., JWT token) with their role to redirect them appropriately.

**Scenario 2: Incorrect Credentials (Unhappy Flow)**
* **Given** the user provides an incorrect username or password
* **When** the user submits their credentials
* **Then** the system denies access
* **And** displays a generic "Invalid username or password" message without locking the account.

**Scenario 3: Inactive User (Unhappy Flow)**
* **Given** an existing user account that is marked as inactive
* **When** the user submits their correct credentials
* **Then** the system denies access
* **And** displays the message "Account is inactive, please contact support."

---

## # Post-implementation Validation

1. `mvn clean install`
2. `mvn test` verifying authentication cases
3. Local startup and Postman/Swagger verification of `/api/auth/login` endpoint.

---

## # Non-functional notes

- Ensure passwords are not logged in any system logs.
- Prevent timing attacks by using a consistent time comparison for passwords (e.g., BCrypt).

---
