# Solution Diagrams — RF-03

This document details the diagrams of the proposed solution to understand the implementation.

---

## 1. Sequence Diagram — Self-Service Password Reset (spec-01)

Shows the flow for a user who requests a new provisional password by providing their username and email.

```mermaid
sequenceDiagram
    actor User as User
    participant AC as AuthController
    participant AS as AuthService
    participant UR as UsuarioRepository
    participant PE as PasswordEncoder
    participant DB as Database (usuarios)
    participant NOTIF as Notification Channel
    note over NOTIF: (pending B1: email / on-screen)

    User->>AC: POST /api/auth/reset-password<br/>{ username, correo }
    AC->>AC: Validate fields (not blank)
    alt Missing fields
        AC-->>User: 400 Bad Request
    end

    AC->>AS: resetPassword(username, correo)
    AS->>UR: findByUsernameAndCorreo(username, correo)
    UR->>DB: SELECT * FROM usuarios WHERE username=? AND correo=?
    DB-->>UR: Optional<Usuario>

    alt User not found or email mismatch
        AS-->>AC: throw UserNotFoundException
        AC-->>User: 404 "No account found matching the provided credentials."
    else User found but activo = false
        AS-->>AC: throw AccountInactiveException
        AC-->>User: 403 "Account is inactive, please contact support."
    else User found and active
        AS->>AS: generateProvisionalPassword() → plainText
        AS->>PE: encode(plainText) → hash
        AS->>UR: save(usuario with hash + debeCambiarContrasenia=true)
        UR->>DB: UPDATE usuarios SET contrasenia=hash, debe_cambiar=true WHERE id=?
        DB-->>UR: OK
        AS->>NOTIF: deliver(plainText, user.correo)
        note over NOTIF: Delivery pending B1 decision
        AS-->>AC: success
        AC-->>User: 200 OK { "message": "Provisional password issued." }
    end
```

---

## 2. Sequence Diagram — Admin-Driven Password Reset (spec-02)

Shows the flow for a Global Admin resetting another user's password from the management screen.

```mermaid
sequenceDiagram
    actor Admin as Global Admin
    participant SF as Spring Security Filter
    participant AC as AuthController
    participant AS as AuthService
    participant UR as UsuarioRepository
    participant PE as PasswordEncoder
    participant DB as Database (usuarios)
    participant NOTIF as Notification Channel
    note over NOTIF: (pending B1: email to target user)

    Admin->>SF: POST /api/auth/admin-reset-password/{userId}<br/>[Authorization: Bearer JWT]
    SF->>SF: Validate JWT & check role = ADMIN_GLOBAL
    alt Not authenticated
        SF-->>Admin: 401 Unauthorized
    else Role != ADMIN_GLOBAL
        SF-->>Admin: 403 Forbidden
    end

    SF->>AC: Forward request (authorized)
    AC->>AS: adminResetPassword(userId)
    AS->>UR: findById(userId)
    UR->>DB: SELECT * FROM usuarios WHERE id=?
    DB-->>UR: Optional<Usuario>

    alt User not found
        AS-->>AC: throw UserNotFoundException
        AC-->>Admin: 404 "User not found."
    else User inactive
        AS-->>AC: throw AccountInactiveException
        AC-->>Admin: 409 "Cannot reset password for an inactive account."
    else User found and active
        AS->>AS: generateProvisionalPassword() → plainText
        AS->>PE: encode(plainText) → hash
        AS->>UR: save(usuario with hash + debeCambiarContrasenia=true)
        UR->>DB: UPDATE usuarios SET contrasenia=hash, debe_cambiar=true WHERE id=?
        DB-->>UR: OK
        AS->>NOTIF: deliver(plainText, targetUser.correo)
        note over NOTIF: Admin never receives plain text
        AS-->>AC: success
        AC-->>Admin: 200 OK { "message": "Password reset successfully for user {userId}." }
    end
```

---

## 3. Class Diagram — Static Design

Shows the classes, interfaces, attributes, and methods involved in the solution.

```mermaid
classDiagram
    class AuthController {
        +resetPassword(PasswordResetRequest, HttpServletRequest) ResponseEntity
        +adminResetPassword(Integer userId, HttpServletRequest) ResponseEntity
    }

    class AuthService {
        -usuarioRepository UsuarioRepository
        -passwordEncoder PasswordEncoder
        +resetPassword(username String, correo String) void
        +adminResetPassword(userId Integer) void
        -generateProvisionalPassword() String
    }

    class UsuarioRepository {
        <<interface>>
        +findByUsername(String) Optional~Usuario~
        +findByUsernameAndCorreo(String, String) Optional~Usuario~
        +findById(Integer) Optional~Usuario~
    }

    class Usuario {
        -id Integer
        -username String
        -contrasenia String
        -correo String
        -nombre String
        -apellido String
        -activo Boolean
        -debeCambiarContrasenia Boolean
        -rol Rol
    }

    class PasswordResetRequest {
        +username String
        +correo String
    }

    class Rol {
        -id Integer
        -nombre String
    }

    class PasswordEncoder {
        <<interface>>
        +encode(rawPassword) String
        +matches(rawPassword, encodedPassword) boolean
    }

    AuthController --> AuthService : uses
    AuthService --> UsuarioRepository : uses
    AuthService --> PasswordEncoder : uses
    UsuarioRepository --> Usuario : manages
    Usuario --> Rol : has
    AuthController ..> PasswordResetRequest : accepts
```

---

## 4. State Diagram — User Account States During Reset Flow

Shows how a `Usuario` transitions between states through the password reset lifecycle.

```mermaid
stateDiagram-v2
    [*] --> ACTIVE : Account created / registered

    ACTIVE --> PENDING_PASSWORD_CHANGE : Password reset triggered\n(self-service or admin)
    note right of PENDING_PASSWORD_CHANGE
        debeCambiarContrasenia = true\nProvisional password stored (BCrypt hash)
    end note

    PENDING_PASSWORD_CHANGE --> ACTIVE : User logs in with provisional password\nand changes to a new permanent password
    note right of ACTIVE
        debeCambiarContrasenia = false\nNew BCrypt hash stored
    end note

    ACTIVE --> INACTIVE : Admin deactivates account\n(activo = false)
    INACTIVE --> ACTIVE : Admin reactivates account

    INACTIVE --> [*] : Account deleted (out of scope)

    PENDING_PASSWORD_CHANGE --> EXPIRED : Provisional password expires after N hours\n(pending B2 option C)
    EXPIRED --> PENDING_PASSWORD_CHANGE : User requests a new reset
```
