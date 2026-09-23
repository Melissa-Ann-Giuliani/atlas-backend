# Solution Diagrams - RF-30

## 1. Sequence Diagram
This diagram illustrates the interaction flow when an Administrator registers a new Teacher.

```mermaid
sequenceDiagram
    actor Admin as Administrator
    participant API as TeacherController
    participant Service as TeacherService
    participant Repo as TeacherRepository
    participant Email as EmailService
    
    Admin->>API: POST /api/teachers (TeacherDTO)
    activate API
    API->>Service: registerTeacher(TeacherDTO)
    activate Service
    
    Service->>Repo: existsByDniOrEmail(dni, email)
    activate Repo
    Repo-->>Service: false
    deactivate Repo
    
    Service->>Service: calculateSeniority(category, priorSeniority)
    Service->>Service: generateTempPassword()
    Service->>Service: hashPassword()
    
    Service->>Repo: save(TeacherEntity)
    activate Repo
    Repo-->>Service: savedEntity
    deactivate Repo
    
    Service->>Email: sendConfirmationEmail(email, tempPassword)
    
    Service-->>API: TeacherResponseDTO
    deactivate Service
    API-->>Admin: 201 Created
    deactivate API
```

## 2. Class Diagram
This diagram outlines the core entities and services involved.

```mermaid
classDiagram
    class TeacherController {
        +registerTeacher(dto: TeacherDTO): ResponseEntity
    }
    
    class TeacherService {
        +registerTeacher(dto: TeacherDTO): TeacherResponseDTO
        -calculateSeniority(category: String, prior: int): int
        -generateTempPassword(): String
    }
    
    class TeacherRepository {
        +existsByDni(dni: String): boolean
        +existsByEmail(email: String): boolean
        +save(teacher: TeacherEntity): TeacherEntity
    }
    
    class TeacherEntity {
        +String username
        +String firstName
        +String lastName
        +String dni
        +String cuil
        +String email
        +int seniority
        +LocalDate joinDate
        +String password
    }
    
    class EmailService {
        +sendConfirmationEmail(to: String, tempPassword: String): void
    }
    
    TeacherController --> TeacherService
    TeacherService --> TeacherRepository
    TeacherService --> EmailService
    TeacherRepository --> TeacherEntity
```
