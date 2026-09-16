# Solution Diagrams — RF-01

This document details the diagrams of the proposed solution to understand the implementation.

---

## 1. Sequence Diagram (Mandatory)

Shows the sequence flow of the solution being specified, illustrating the interaction between actors, controllers, services, repositories, and external systems.

```mermaid
sequenceDiagram
    actor User as User
    User->>AuthController: POST /api/auth/login (username, password)
    AuthController->>AuthService: authenticate(username, password)
    AuthService->>UserRepository: findByUsername(username)
    alt User not found or inactive
        UserRepository-->>AuthService: return empty / inactive status
        AuthService-->>AuthController: throw AuthenticationException
        AuthController-->>User: 401 Unauthorized
    else User found and active
        UserRepository-->>AuthService: return User entity
        AuthService->>PasswordEncoder: matches(rawPassword, encodedPassword)
        alt Password mismatch
            PasswordEncoder-->>AuthService: return false
            AuthService-->>AuthController: throw AuthenticationException
            AuthController-->>User: 401 Unauthorized
        else Password matches
            PasswordEncoder-->>AuthService: return true
            AuthService->>JwtService: generateToken(user)
            JwtService-->>AuthService: return JWT
            AuthService-->>AuthController: return AuthResponse(JWT)
            AuthController-->>User: 200 OK + JWT
        end
    end
```

---

## 2. Class Diagram (Mandatory)

Shows the classes, interfaces, attributes, and methods involved in the solution and their relationships.

```mermaid
classDiagram
    class AuthController {
        +login(AuthRequest) AuthResponse
    }
    class AuthService {
        +authenticate(String, String) AuthResponse
    }
    class UserRepository {
        +findByUsername(String) Optional~User~
    }
    class JwtService {
        +generateToken(User) String
    }
    class User {
        -String username
        -String password
        -boolean active
        -String role
    }
    
    AuthController --> AuthService
    AuthService --> UserRepository
    AuthService --> JwtService
    AuthService --> User
```

---

## 3. Additional Useful Diagram (Optional / Space for other diagram)

```mermaid
stateDiagram-v2
    [*] --> Anonymous
    Anonymous --> Authenticated : Valid Login
    Anonymous --> Anonymous : Invalid Login
    Authenticated --> Anonymous : Logout
```
