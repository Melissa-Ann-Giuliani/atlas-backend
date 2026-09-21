# Solution Diagrams — RF-28

This document details the diagrams of the proposed solution to understand the implementation.

---

## 1. Sequence Diagram (Mandatory)

Shows the sequence flow of the solution being specified, illustrating the interaction between actors, controllers, services, repositories, and external systems.

```mermaid
sequenceDiagram
    actor GlobalAdmin
    participant UI as Frontend
    participant AuthController
    participant UserService
    participant EmailService
    participant DB as Database

    GlobalAdmin->>UI: Fill and submit new user form
    UI->>AuthController: POST /api/users
    AuthController->>UserService: createUser(data)
    UserService->>DB: Check if user exists (email/username)
    alt User already exists
        DB-->>UserService: true
        UserService-->>AuthController: Error: User exists
        AuthController-->>UI: 400 Bad Request
        UI-->>GlobalAdmin: Show error message
    else User is new
        UserService->>UserService: Generate temp password & Hash it
        UserService->>DB: Insert new user (Status: Active, Role: Unit Admin)
        DB-->>UserService: Success (UserId)
        UserService->>EmailService: sendWelcomeEmail(email, tempPassword)
        EmailService-->>UserService: Email queued/sent
        UserService-->>AuthController: User created successfully
        AuthController-->>UI: 201 Created
        UI-->>GlobalAdmin: Show success message & update list
    end
```

---

## 2. Class Diagram (Mandatory)

Shows the classes, interfaces, attributes, and methods involved in the solution and their relationships.

```mermaid
classDiagram
    class AuthController {
        +registerUser(RegisterUserDto dto) Response
    }
    class UserService {
        +createUser(RegisterUserDto dto) User
        -generateTempPassword() String
        -hashPassword(String password) String
    }
    class EmailService {
        +sendWelcomeEmail(String email, String tempPassword) void
    }
    class UserRepository {
        +existsByEmailOrUsername(String email, String username) boolean
        +save(User user) User
    }
    class User {
        +UUID id
        +String username
        +String email
        +String firstName
        +String lastName
        +String role
        +String position
        +String dependency
        +String status
        +String passwordHash
    }

    AuthController --> UserService
    UserService --> UserRepository
    UserService --> EmailService
    UserRepository --> User
```

---

## 3. Additional Useful Diagram (State Diagram)

```mermaid
stateDiagram-v2
    [*] --> PendingCreation
    PendingCreation --> Active: Valid Data & New User
    PendingCreation --> Error: User Exists
    Active --> [*]
```
