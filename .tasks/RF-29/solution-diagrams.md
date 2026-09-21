# Solution Diagrams — RF-29

This document details the diagrams of the proposed solution to understand the implementation.

---

## 1. Sequence Diagram (Mandatory)

Shows the sequence flow of the solution being specified, illustrating the interaction between actors, controllers, services, repositories, and external systems.

```mermaid
sequenceDiagram
    actor Admin as Global Administrator
    participant UI as Frontend
    participant Ctrl as UserController
    participant Svc as UserService
    participant DB as Database
    participant Email as EmailService

    Admin->>UI: Fill registration form & Submit
    UI->>Ctrl: POST /api/users (Auditor details)
    Ctrl->>Svc: registerAuditor(dto)
    
    Svc->>DB: checkIfExists(username, email)
    DB-->>Svc: exists = false
    
    Svc->>Svc: generateTemporaryPassword()
    Svc->>Svc: hashPassword()
    
    Svc->>DB: save(newUser with role Auditor, state Active)
    DB-->>Svc: savedUser
    
    Svc->>Email: sendWelcomeEmail(savedUser.email, tempPassword)
    Email-->>Svc: emailSent
    
    Svc-->>Ctrl: AuditorDTO
    Ctrl-->>UI: 201 Created
    UI-->>Admin: Show Success Message
```

---

## 2. Class Diagram (Mandatory)

Shows the classes, interfaces, attributes, and methods involved in the solution and their relationships.

```mermaid
classDiagram
    class UserController {
        +registerAuditor(dto: UserRegistrationDTO): ResponseEntity
    }
    
    class UserService {
        +registerAuditor(dto: UserRegistrationDTO): User
        -generateTemporaryPassword(): String
        -checkIfUserExists(username: String, email: String): boolean
    }
    
    class EmailService {
        +sendWelcomeEmail(to: String, tempPassword: String): void
    }
    
    class UserRepository {
        +existsByUsername(username: String): boolean
        +existsByEmail(email: String): boolean
        +save(user: User): User
    }
    
    class User {
        -id: Long
        -username: String
        -email: String
        -firstName: String
        -lastName: String
        -role: Role
        -status: UserStatus
        -facultyPosition: String
        -passwordHash: String
    }
    
    UserController --> UserService
    UserService --> UserRepository
    UserService --> EmailService
    UserService ..> User : creates
```

---

## 3. State Diagram

```mermaid
stateDiagram-v2
    [*] --> Active : Admin registers Auditor
    Active --> PasswordChanged : First login (force password change)
```
