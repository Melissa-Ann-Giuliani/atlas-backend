# Solution Diagrams — RF-39

This document details the diagrams of the proposed solution to understand the implementation of the Change History (RF-39).

---

## 1. Sequence Diagram (Mandatory)

Flow for querying the change history (`GET /api/cambios`) with RBAC unit scoping and dynamic filtering.

```mermaid
sequenceDiagram
    actor User as Admin Global / Admin Unidad / Auditor
    participant Ctrl as CambioController
    participant Svc as CambioService
    participant Sec as SecurityContext / UsuarioService
    participant Repo as CambioRepository
    participant DB as PostgreSQL (cambios)

    User->>Ctrl: GET /api/cambios?usuarioAfectado&fechaDesde&fechaHasta&tipoOperacion&usuarioId&page&size
    Ctrl->>Svc: consultarHistorial(filtros, pageable)
    Svc->>Sec: getAuthenticatedUsuario()
    Sec-->>Svc: Usuario (rol, unidadId)

    alt fechaDesde > fechaHasta
        Svc-->>Ctrl: throw IllegalArgumentException (400 Bad Request)
        Ctrl-->>User: 400 Bad Request
    else Valid Request
        alt rol == ADMIN_UNIDAD
            Svc->>Svc: Force unidadId = usuario.unidadId & usuarioAfectado != null
        end
        Svc->>Repo: findAll(CambioSpecification, pageable [fechaHora DESC])
        Repo->>DB: SELECT c.*, u.*, ua.* FROM cambios c ... ORDER BY cambio_fecha_hora DESC
        DB-->>Repo: Page<Cambio>
        Repo-->>Svc: Page<Cambio>
        Svc->>Svc: Map to Page<CambioResponseDTO>
        Svc-->>Ctrl: Page<CambioResponseDTO>
        Ctrl-->>User: 200 OK (Page<CambioResponseDTO>)
    end
```

---

## 2. Class Diagram (Mandatory)

Static structure of the `cambio` package and its relationships with `Usuario` (responsable y afectado) and `Unidad`.

```mermaid
classDiagram
    class TipoOperacion {
        <<enumeration>>
        ALTA
        MODIFICACION
        BAJA
    }

    class Cambio {
        -Integer id
        -String tablaAfectada
        -String campoAfectado
        -String valorAnterior
        -String valorNuevo
        -LocalDateTime fechaHora
        -TipoOperacion tipoOperacion
        -Usuario usuario
        -Usuario usuarioAfectado
        -Unidad unidad
    }

    class CambioResponseDTO {
        -Integer id
        -LocalDateTime fechaHora
        -UsuarioResumenDTO usuarioResponsable
        -String categoria
        -String campoAfectado
        -String valorAnterior
        -String valorNuevo
        -String tipoOperacion
        -Integer usuarioAfectadoId
        -String usuarioAfectadoNombreCompleto
        -Integer unidadId
    }

    class CambioController {
        -CambioService cambioService
        +consultarHistorial(usuarioAfectado, fechaDesde, fechaHasta, tipoOperacion, usuarioId, pageable) ResponseEntity~Page~CambioResponseDTO~~
    }

    class CambioService {
        -CambioRepository repository
        +registrarCambio(tabla, campo, valorAnt, valorNuevo, tipoOp, usuarioResponsable, usuarioAfectado, unidad) void
        +consultarHistorial(filtros, pageable) Page~CambioResponseDTO~
    }

    class CambioRepository {
        <<interface>>
        +findAll(Specification~Cambio~, Pageable) Page~Cambio~
    }

    CambioController --> CambioService
    CambioService --> CambioRepository
    CambioService ..> CambioResponseDTO : maps to
    CambioRepository --> Cambio
    Cambio --> TipoOperacion
    Cambio --> Usuario : responsable
    Cambio --> Usuario : afectado (optional)
    Cambio --> Unidad : scope (optional)
```

---

## 3. Additional Useful Diagram — Audit Recording & Consultation Data Flow

Illustrates how mutating services feed `CambioService` and how `CambioController` reads from the same audit store with RBAC filtering.

```mermaid
flowchart LR
    subgraph MutatingServices["Mutating Services (Write Path)"]
        DS["DocenteService"]
        DeS["DesignacionService"]
        LS["LicenciaService / Otros"]
        US["UsuarioService / Admins"]
    end

    CS["CambioService.registrarCambio()"]
    DB[("PostgreSQL: public.cambios")]
    CQ["CambioService.consultarHistorial()"]
    CC["CambioController (GET /api/cambios)"]

    DS --> CS
    DeS --> CS
    LS --> CS
    US --> CS
    CS --> DB
    DB --> CQ
    CQ --> CC
```
