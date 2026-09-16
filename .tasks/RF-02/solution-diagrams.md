# Solution Diagrams — RF-02

This document details the diagrams of the proposed solution to understand the implementation.

---

## 1. Sequence Diagram

Shows the full authentication failure flow through the Atlas backend, from the HTTP request to the error response reaching the client.

```mermaid
sequenceDiagram
    actor User as User (Atlas System User)
    participant Frontend as Frontend Client
    participant AC as AuthController<br/>(POST /api/auth/login)
    participant AS as AuthService
    participant AM as AuthenticationManager<br/>(Spring Security)
    participant UDS as CustomUserDetailsService
    participant DB as usuarios (DB Table)

    User->>Frontend: Enter username + password → Submit
    Frontend->>AC: POST /api/auth/login<br/>{ "username": "...", "password": "..." }

    AC->>AS: authenticate(AuthRequest)
    AS->>AM: authenticate(UsernamePasswordAuthenticationToken)
    AM->>UDS: loadUserByUsername(username)
    UDS->>DB: SELECT * FROM usuarios WHERE usuario_username = ?

    alt Username not found in DB
        DB-->>UDS: (empty result)
        UDS-->>AM: throws UsernameNotFoundException
        Note over AM: Spring Security converts to BadCredentialsException
        AM-->>AS: throws BadCredentialsException
        AS-->>AC: throws Exception("Invalid username or password")
        AC-->>Frontend: HTTP 401 Unauthorized<br/>{ "message": "Invalid username or password" }
        Frontend-->>User: Display error message

    else Username found but password mismatch
        DB-->>UDS: Usuario record
        UDS-->>AM: UserDetails (activo=true, hashed password)
        AM-->>AS: throws BadCredentialsException
        AS-->>AC: throws Exception("Invalid username or password")
        AC-->>Frontend: HTTP 401 Unauthorized<br/>{ "message": "Invalid username or password" }
        Frontend-->>User: Display error message

    else Username found but account inactive (activo = false)
        DB-->>UDS: Usuario record (activo=false)
        UDS-->>AM: UserDetails (enabled=false)
        AM-->>AS: throws DisabledException
        AS-->>AC: throws Exception("Account is inactive, please contact support.")
        AC-->>Frontend: HTTP 401 Unauthorized<br/>{ "message": "Account is inactive, please contact support." }
        Frontend-->>User: Display inactive account message

    else Valid credentials + active account
        DB-->>UDS: Usuario record (activo=true)
        UDS-->>AM: UserDetails (enabled=true, password matches)
        AM-->>AS: Authentication successful
        AS->>UDS: loadUserByUsername(username)
        UDS-->>AS: UserDetails
        AS->>AS: jwtUtil.generateToken(UserDetails)
        AS-->>AC: AuthResponse(token)
        AC-->>Frontend: HTTP 200 OK { "token": "eyJ..." }
        Frontend-->>User: Redirect to Dashboard
    end
```

---

## 2. Class Diagram

Shows the classes, interfaces, and relationships involved in the authentication failure handling.

```mermaid
classDiagram
    class AuthController {
        -AuthService authService
        +login(AuthRequest) ResponseEntity
    }

    class AuthService {
        -AuthenticationManager authenticationManager
        -CustomUserDetailsService userDetailsService
        -JwtUtil jwtUtil
        +authenticate(AuthRequest) AuthResponse
    }

    class AuthRequest {
        -String username
        -String password
        +getUsername() String
        +getPassword() String
    }

    class AuthResponse {
        -String token
        +getToken() String
    }

    class CustomUserDetailsService {
        -UsuarioRepository usuarioRepository
        +loadUserByUsername(String) UserDetails
    }

    class Usuario {
        -Integer id
        -String username
        -String contrasenia
        -String correo
        -String nombre
        -String apellido
        -Boolean activo
        -Rol rol
    }

    class BadCredentialsException {
        <<Spring Security>>
    }

    class DisabledException {
        <<Spring Security>>
    }

    class UsernameNotFoundException {
        <<Spring Security>>
        %% Spring Security converts this to BadCredentialsException internally
    }

    AuthController --> AuthService : delegates to
    AuthController ..> AuthRequest : deserializes
    AuthController ..> AuthResponse : returns
    AuthService --> CustomUserDetailsService : loads user
    AuthService ..> BadCredentialsException : catches
    AuthService ..> DisabledException : catches
    CustomUserDetailsService --> Usuario : maps from
    CustomUserDetailsService ..> UsernameNotFoundException : throws
```

---

## 3. Error Response State Diagram

Shows the possible states an authentication attempt can transition through, and which error message is emitted at each failure state.

```mermaid
stateDiagram-v2
    [*] --> CredentialsReceived : POST /api/auth/login

    CredentialsReceived --> UsernameCheck : AuthenticationManager triggered

    UsernameCheck --> PasswordCheck : Username exists in DB
    UsernameCheck --> AuthFailed_Generic : Username NOT found\n(UsernameNotFoundException → BadCredentialsException)

    PasswordCheck --> AccountStatusCheck : Password matches
    PasswordCheck --> AuthFailed_Generic : Password mismatch\n(BadCredentialsException)

    AccountStatusCheck --> AuthSuccess : activo = true
    AccountStatusCheck --> AuthFailed_Inactive : activo = false\n(DisabledException)

    AuthFailed_Generic --> [*] : HTTP 401\n"Invalid username or password"
    AuthFailed_Inactive --> [*] : HTTP 401\n"Account is inactive,\nplease contact support."
    AuthSuccess --> [*] : HTTP 200\nJWT Token issued
```
