# Solution Diagrams — RF-09: Registrar las designaciones de docentes

This document details the diagrams of the proposed solution for the teacher designation registration workflow.

---

## 1. Sequence Diagram (Mandatory)

Shows the sequence flow of the solution being specified, illustrating the interaction between the client actor, security filters, controller, service, repositories, and notification service.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Administrador (Global / Unidad)
    participant SEC as Spring Security Filter
    participant DC as DesignacionController
    participant DS as DesignacionService
    participant DR as DocenteRepository
    participant CR as CargoRepository
    participant DesR as DesignacionRepository
    participant ES as EmailService

    Admin->>SEC: POST /api/designaciones (payload con docenteId, cargoId, nroResolucion, fechas)
    alt Rol no autorizado (e.g. AUDITOR o ANONIMO)
        SEC-->>Admin: 403 Forbidden (Decisión B2)
    else Rol autorizado (ADMIN_GLOBAL / ADMIN_UNIDAD)
        SEC->>DC: crearDesignacion(request, authentication)
        DC->>DS: registrarDesignacion(request, username)

        DS->>DR: findById(docenteId)
        alt Docente no encontrado
            DR-->>DS: Optional.empty()
            DS-->>DC: ResponseStatusException(404, "Docente no encontrado")
            DC-->>Admin: 404 Not Found (Redirección a RF-30 en UI)
        end
        DR-->>DS: Docente existente

        DS->>DesR: existsByUsuarioIdAndDesignacionNumeroResolucion(docenteId, nroResolucion)
        alt Resolución ya registrada para el docente
            DesR-->>DS: true
            DS-->>DC: ResponseStatusException(400, "El docente ya posee una designación con dicho número")
            DC-->>Admin: 400 Bad Request (Decisión R3)
        end
        DesR-->>DS: false

        alt Validación de fechas inválida (fechaFin < fechaInicio)
            DS-->>DC: ResponseStatusException(400, "La fecha de finalización no puede ser anterior a la de inicio")
            DC-->>Admin: 400 Bad Request
        end

        DS->>CR: findById(cargoId)
        alt Cargo no encontrado
            CR-->>DS: Optional.empty()
            DS-->>DC: ResponseStatusException(404, "Cargo no encontrado")
            DC-->>Admin: 404 Not Found
        end
        CR-->>DS: Cargo existente

        alt ADMIN_UNIDAD y Cargo de otra unidad
            DS-->>DC: AccessDeniedException(403, "No tiene permisos para cargos de otra unidad")
            DC-->>Admin: 403 Forbidden (Decisión R1)
        end

        alt Cargo no está en estado "Libre"
            DS-->>DC: ResponseStatusException(400, "El cargo ya se encuentra asignado o en licencia")
            DC-->>Admin: 400 Bad Request
        end

        %% Persistencia atómica
        Note over DS, DesR: Transacción atómica (@Transactional)
        DS->>DesR: save(nuevaDesignacion con estado "Activo")
        DesR-->>DS: Designacion guardada

        DS->>CR: cargo.setDesignacion(designacion), cargo.setEstado("Asignado")
        CR-->>DS: Cargo persistido

        DS->>DR: docente.setEstado("Activo"), usuario.setActivo(true)
        DR-->>DS: Docente persistido

        %% Notificación asíncrona fail-safe
        DS->>ES: sendDesignacionNotification(docenteEmail, nroResolucion, ...)
        Note over ES: Envío asíncrono fail-safe (@Async, Decisión R2)

        DS-->>DC: DesignacionResponseDTO
        DC-->>Admin: 201 Created (DTO enriquecido para comprobante / impresión, Decisión B3)
    end
```

---

## 2. Class Diagram (Mandatory)

Shows the classes, DTOs, controllers, services, repositories, and entities involved in the solution and their relationships.

```mermaid
classDiagram
    class DesignacionController {
        -DesignacionService designacionService
        +crearDesignacion(DesignacionCreateRequest request, Authentication auth): ResponseEntity~DesignacionResponseDTO~
        +obtenerDesignacionPorId(Integer id): ResponseEntity~DesignacionResponseDTO~
    }

    class DesignacionService {
        -DesignacionRepository designacionRepository
        -DocenteRepository docenteRepository
        -CargoRepository cargoRepository
        -UsuarioRepository usuarioRepository
        -EmailService emailService
        +registrarDesignacion(DesignacionCreateRequest request, String username): DesignacionResponseDTO
        +obtenerPorId(Integer id): DesignacionResponseDTO
    }

    class DesignacionCreateRequest {
        +Integer docenteId
        +Integer cargoId
        +Integer nroResolucion
        +LocalDate fechaInicio
        +LocalDate fechaFin
    }

    class DesignacionResponseDTO {
        +Integer id
        +Integer nroResolucion
        +LocalDate fechaInicio
        +LocalDate fechaFin
        +String estado
        +DocenteResumenDTO docente
        +CargoResumenDTO cargo
    }

    class Designacion {
        +Integer designacionId
        +Integer designacionNumeroResolucion
        +LocalDate designacionFechaInicio
        +LocalDate designacionFechaFin
        +String designacionEstadoActual
        +Integer usuarioId
    }

    class Cargo {
        +Integer id
        +Long codigo
        +Integer horasOcupadas
        +LocalDate fechaCreacion
        +String estado
        +Designacion designacion
        +Unidad unidad
    }

    class Docente {
        +Long dni
        +Long cuil
        +String emailInstitucional
        +LocalDate fechaIngreso
        +String estado
    }

    class EmailService {
        +sendDesignacionNotification(String to, Integer resolucion, LocalDate inicio, LocalDate fin): void
    }

    class DesignacionRepository {
        +findByUsuarioId(Integer usuarioId): List~Designacion~
        +existsByUsuarioIdAndDesignacionNumeroResolucion(Integer usuarioId, Integer resolucion): boolean
    }

    DesignacionController --> DesignacionService : uses
    DesignacionController ..> DesignacionCreateRequest : receives
    DesignacionController ..> DesignacionResponseDTO : returns
    DesignacionService --> DesignacionRepository : uses
    DesignacionService --> EmailService : notifies
    Cargo --> Designacion : references
    Designacion --> Docente : belongs to (usuario_id)
```

---

## 3. State Machine Diagram (Lifecycle of Teacher and Cargo during Designation)

Illustrates the state transitions of the Teacher (`Docente`), the Position (`Cargo`), and the Designation (`Designacion`) when RF-09 is executed.

```mermaid
stateDiagram-v2
    direction LR

    state "Docente" as Doc {
        [*] --> Inactivo : Registro inicial (RF-30)
        Inactivo --> Activo : RF-09 Registrar Designación
        Activo --> Licencia : Asignación Licencia
        Licencia --> Activo : Fin de Licencia
        Activo --> Inactivo : Baja/Cese
    }

    state "Cargo" as Car {
        [*] --> Libre : Creación de Cargo (RF-17)
        Libre --> Asignado : RF-09 Registrar Designación
        Asignado --> Libre : Desvinculación / Cese
        Asignado --> Licencia : Licencia de Docente
    }

    state "Designacion" as Des {
        [*] --> Activo : RF-09 Creación
        Activo --> Licencia : Durante Licencia
        Activo --> Finalizada : Cumplimiento fecha fin
        Activo --> Baja : Baja de Designación
    }
```
