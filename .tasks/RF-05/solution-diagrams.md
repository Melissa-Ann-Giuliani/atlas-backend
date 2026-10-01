# Solution Diagrams — RF-05

This document details the diagrams of the proposed solution to understand the implementation.

---

## 1. Sequence Diagram (Mandatory)

Shows the sequence flow of the solution being specified, illustrating the interaction between actors, controllers, services, repositories, and external systems.

```mermaid
sequenceDiagram
    actor User as Admin/Auditor
    participant FE as Frontend Client
    participant Ctrl as TeacherController
    participant Svc as TeacherService
    participant Repo as TeacherRepository
    participant DB as Database

    User->>FE: Navigate to "Docentes" list
    FE->>Ctrl: GET /api/teachers?status=active
    Ctrl->>Svc: getActiveTeachers(userRole, userUnitId)
    Svc->>Repo: findActiveTeachersWithFilters(...)
    Repo->>DB: Execute Query
    DB-->>Repo: Return Resultset
    Repo-->>Svc: List<TeacherDTO>
    Svc-->>Ctrl: List<TeacherDTO>
    Ctrl-->>FE: 200 OK + JSON
    FE-->>User: Render Data Table
```

---

## 2. Class Diagram (Mandatory)

Shows the classes, interfaces, attributes, and methods involved in the solution and their relationships.

```mermaid
classDiagram
    class TeacherController {
        +getActiveTeachers(page, size, filters)
        +getTeacherDetails(id)
    }
    class TeacherService {
        +findActiveTeachers(UserContext, filters)
        +getTeacherProfile(id)
    }
    class TeacherRepository {
        <<interface>>
        +findActiveByUnitId(unitId)
        +findAllActive()
    }
    
    TeacherController --> TeacherService
    TeacherService --> TeacherRepository
```

---

## 3. Additional Useful Diagram (Optional / Space for other diagram)

Use this space to add another diagram that helps understand the solution to implement.

```mermaid
flowchart TD
    A[User accesses List] --> B{What is User Role?}
    B -->|Global Admin| C[Query all active teachers]
    B -->|Auditor| C
    B -->|Unit Admin| D[Query active teachers by Unit ID]
    C --> E[Apply Table Filters]
    D --> E
    E --> F[Return Paginated Results]
```
