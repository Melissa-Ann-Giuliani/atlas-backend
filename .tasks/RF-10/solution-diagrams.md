# Solution Diagrams — RF-10: Vincular un cargo a una designación de un docente

This document details the diagrams of the proposed solution for linking an existing `"Libre"` position (`Cargo`) to a teacher's designation (`Designacion`).

---

## 1. Sequence Diagram (Mandatory)

Shows the end-to-end interaction flow between the administrator, the cargo listing/creation endpoints (RF-06 / RF-17), and the designation-cargo linking endpoint (`PATCH /api/designaciones/{id}/cargo`).

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Administrador (Global / Unidad)
    participant SEC as Spring Security Filter
    participant CC as CargoController
    participant DC as DesignacionController
    participant DS as DesignacionService
    participant DesR as DesignacionRepository
    participant CR as CargoRepository
    participant DR as DocenteRepository

    Note over Admin, CC: Paso previo: Consultar cargos en estado "Libre" (RF-06, Decisión R2) o crear uno nuevo (RF-17, Decisión M1)
    Admin->>CC: GET /api/cargos?estado=Libre
    CC-->>Admin: 200 OK (Page<CargoListDTO> con cargos "Libre" de su alcance)

    opt Si necesita un nuevo cargo (Botón "Crear Cargo" -> RF-17)
        Admin->>CC: POST /api/cargos (CargoCreateRequest)
        CC-->>Admin: 201 Created (Nuevo cargo en estado "Libre")
    end

    Note over Admin, DR: Vinculación del cargo a la designación (RF-10, Decisión B1)
    Admin->>SEC: PATCH /api/designaciones/{id}/cargo ({ "cargoId": 25 })
    alt Rol no autorizado (e.g. AUDITOR o ANONIMO)
        SEC-->>Admin: 403 Forbidden
    else Rol autorizado (ADMIN_GLOBAL / ADMIN_UNIDAD)
        SEC->>DC: vincularCargo(id, request, authentication)
        DC->>DS: vincularCargo(id, request.getCargoId(), username)

        DS->>DesR: findById(id)
        alt Designación no encontrada
            DesR-->>DS: Optional.empty()
            DS-->>DC: ResponseStatusException(404, "Designación no encontrada")
            DC-->>Admin: 404 Not Found
        end
        DesR-->>DS: Designacion existente

        DS->>CR: findByDesignacionDesignacionIdIn(List.of(id))
        alt La designación ya posee un cargo vinculado (Decisión B2)
            CR-->>DS: List con cargo existente
            DS-->>DC: ResponseStatusException(400, "La designación ya posee un cargo vinculado")
            DC-->>Admin: 400 Bad Request
        end
        CR-->>DS: Lista vacía

        DS->>CR: findById(cargoId)
        alt Cargo no encontrado (Solo cargos ya creados)
            CR-->>DS: Optional.empty()
            DS-->>DC: ResponseStatusException(404, "Cargo no encontrado")
            DC-->>Admin: 404 Not Found
        end
        CR-->>DS: Cargo existente

        alt Actor es ADMIN_UNIDAD y Cargo pertenece a otra unidad (Decisión R3)
            DS-->>DC: AccessDeniedException("No tiene permisos para vincular cargos de otra unidad")
            DC-->>Admin: 403 Forbidden
        end

        alt Cargo no está en estado "Libre"
            DS-->>DC: ResponseStatusException(400, "El cargo ya se encuentra asignado o en licencia")
            DC-->>Admin: 400 Bad Request
        end

        Note over DS, DR: Transacción atómica (@Transactional)
        DS->>CR: cargo.setDesignacion(designacion), cargo.setEstado("Asignado"), save(cargo)
        CR-->>DS: Cargo actualizado ("Asignado")

        DS->>DR: findById(designacion.getUsuarioId())
        DR-->>DS: Docente

        Note over DS: El envío de email se difiere hasta RF-11 (Decisión R1)

        DS-->>DC: DesignacionResponseDTO (con DocenteResumenDTO y CargoResumenDTO)
        DC-->>Admin: 200 OK (DesignacionResponseDTO)
    end
```

---

## 2. Class Diagram (Mandatory)

Shows the classes, DTOs, controllers, services, repositories, and entities involved in linking a `Cargo` to a `Designacion`.

```mermaid
classDiagram
    class DesignacionController {
        -DesignacionService designacionService
        +crearDesignacion(DesignacionCreateRequest request, Authentication auth): ResponseEntity~?~
        +vincularCargo(Integer id, DesignacionVincularCargoRequest request, Authentication auth): ResponseEntity~?~
        +obtenerDesignacionPorId(Integer id): ResponseEntity~?~
    }

    class DesignacionService {
        -DesignacionRepository designacionRepository
        -DocenteRepository docenteRepository
        -CargoRepository cargoRepository
        -UsuarioRepository usuarioRepository
        -AdminUnidadRepository adminUnidadRepository
        +registrarDesignacion(DesignacionCreateRequest request, String username): DesignacionResponseDTO
        +vincularCargo(Integer designacionId, Integer cargoId, String username): DesignacionResponseDTO
        +obtenerPorId(Integer id): DesignacionResponseDTO
    }

    class DesignacionVincularCargoRequest {
        +Integer cargoId
    }

    class DesignacionCreateRequest {
        +Integer docenteId
        +Integer cargoId
        +String nroResolucion
        +LocalDate fechaInicio
        +LocalDate fechaFin
    }

    class DesignacionResponseDTO {
        +Integer id
        +String nroResolucion
        +LocalDate fechaInicio
        +LocalDate fechaFin
        +String estado
        +DocenteResumenDTO docente
        +CargoResumenDTO cargo
    }

    class Designacion {
        +Integer designacionId
        +String designacionNumeroResolucion
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
        +Integer id
        +Integer legajo
        +Long dni
        +String emailInstitucional
        +String estado
        +Boolean activo
    }

    DesignacionController --> DesignacionService : uses
    DesignacionController ..> DesignacionVincularCargoRequest : receives
    DesignacionController ..> DesignacionResponseDTO : returns
    DesignacionService --> Cargo : links & sets "Asignado"
    DesignacionService --> Designacion : reads
    Cargo --> Designacion : designacion_id (FK)
    Designacion --> Docente : usuario_id (FK)
```

---

## 3. User Workflow & State Transition Diagram

Illustrates the complete administrative journey across RF-09 (Create Designation), RF-17 (Create Cargo if needed), RF-10 (Link Cargo to Designation), and RF-11 (Assign Functions & Final Notification).

```mermaid
flowchart TD
    Start(["Inicio: Trámites > Designaciones"]) --> Step1["Ingresar datos del Docente y Designación (RF-09)"]
    Step1 --> ClickAssign["Seleccionar botón 'Asignar cargo' (RF-10)"]
    ClickAssign --> FetchFree["Consultar lista de cargos en estado 'Libre' (GET /api/cargos?estado=Libre)"]
    FetchFree --> NeedNew{"¿Existe el cargo requerido en la lista?"}

    NeedNew -- "No (Botón 'Crear Cargo')" --> RedirectRF17["Redirección a Gestión > Alta de Cargo (RF-17: POST /api/cargos)"]
    RedirectRF17 --> CreatedFree["Nuevo Cargo creado con estado = 'Libre'"]
    CreatedFree --> FetchFree

    NeedNew -- "Sí" --> SelectCargo["Elegir cargo en estado 'Libre' de la lista"]
    SelectCargo --> LinkAPI["Vincular Cargo a Designación (PATCH /api/designaciones/{id}/cargo o POST /api/designaciones)"]
    LinkAPI --> Validate{"¿Cargo existe, está 'Libre' y pertenece a la Unidad?"}

    Validate -- "No" --> Error["Rechazar con HTTP 400 / 403 / 404"]
    Validate -- "Sí" --> Persist["Transacción: Cargo.designacion = Designacion, Cargo.estado = 'Asignado'"]
    Persist --> NextRF11["Siguiente etapa (RF-11): Asignar funciones al cargo y enviar notificación final al docente"]
```
