# Solution Diagrams — RF-33

This document details the diagrams of the proposed solution for modifying Unit Administrators.

---

## 1. Sequence Diagram (Mandatory)

Shows the sequence flow when a Global Administrator updates an Admin de Unidad profile, cargo, and academic unit.

```mermaid
sequenceDiagram
    autonumber
    actor AG as Global Administrator
    participant UC as UsuarioController
    participant AUS as AdminUnidadService
    participant AUR as AdminUnidadRepository
    participant UR as UsuarioRepository
    participant UNR as UnidadRepository
    participant CS as CambioService
    participant CR as CambioRepository

    AG->>UC: PUT /api/usuarios/admin-unidad/{id} (AdminUnidadUpdateRequest payload)
    Note over UC: PreAuthorize: hasRole('ADMIN_GLOBAL') or hasRole('ADMINISTRADOR_GLOBAL')
    UC->>AUS: modificarAdminUnidad(id, request, responsibleUsername)

    AUS->>UR: findByUsername(responsibleUsername)
    UR-->>AUS: responsibleUsuario

    AUS->>AUR: findById(id)
    alt AdminUnidad not found
        AUR-->>AUS: Optional.empty()
        AUS-->>UC: throw ResponseStatusException(404, "Administrador de Unidad no encontrado")
        UC-->>AG: 404 Not Found
    else AdminUnidad found
        AUR-->>AUS: adminUnidad

        opt Username modified
            AUS->>UR: findByUsername(newUsername)
            alt Username already in use by another user
                UR-->>AUS: otherUser (id != currentId)
                AUS-->>UC: throw ResponseStatusException(409, "El nombre de usuario ya se encuentra en uso")
                UC-->>AG: 409 Conflict
            end
        end

        opt Email modified
            AUS->>UR: existsByCorreoAndIdNot(newCorreo, id)
            alt Email already in use by another user
                UR-->>AUS: true
                AUS-->>UC: throw ResponseStatusException(409, "El correo electrónico ya se encuentra en uso")
                UC-->>AG: 409 Conflict
            end
        end

        AUS->>UNR: findById(unidadId)
        alt Unidad not found
            UNR-->>AUS: Optional.empty()
            AUS-->>UC: throw ResponseStatusException(404, "Unidad no encontrada")
            UC-->>AG: 404 Not Found
        else Unidad found
            UNR-->>AUS: nuevaUnidad

            Note over AUS,CS: Audit changes via CambioService
            AUS->>CS: registrarCambio("admin_de_unidad", "username", oldVal, newVal, responsibleUsuario)
            AUS->>CS: registrarCambio("admin_de_unidad", "nombre", oldVal, newVal, responsibleUsuario)
            AUS->>CS: registrarCambio("admin_de_unidad", "apellido", oldVal, newVal, responsibleUsuario)
            AUS->>CS: registrarCambio("admin_de_unidad", "correo", oldVal, newVal, responsibleUsuario)
            AUS->>CS: registrarCambio("admin_de_unidad", "cargo_ad_nombre", oldVal, newVal, responsibleUsuario)
            AUS->>CS: registrarCambio("admin_de_unidad", "unidad_id", oldVal, newVal, responsibleUsuario)

            CS->>CR: save(cambio) [persists only when oldVal != newVal]

            Note over AUS: Update AdminUnidad entity
            AUS->>AUR: save(adminUnidad)
            AUR-->>AUS: updatedAdminUnidad

            AUS-->>UC: updatedAdminUnidad
            UC-->>AG: 200 OK (updatedAdminUnidad)
        end
    end
```

---

## 2. Class Diagram (Mandatory)

Outlines the entities, DTOs, controllers, and services involved in RF-33.

```mermaid
classDiagram
    class UsuarioController {
        -AdminUnidadService adminUnidadService
        +modificarAdminUnidad(id: Integer, request: AdminUnidadUpdateRequest, principal: Principal): ResponseEntity
    }

    class AdminUnidadService {
        -AdminUnidadRepository adminUnidadRepository
        -UsuarioRepository usuarioRepository
        -UnidadRepository unidadRepository
        -CambioService cambioService
        +modificarAdminUnidad(id: Integer, request: AdminUnidadUpdateRequest, authUsername: String): AdminUnidad
    }

    class AdminUnidadUpdateRequest {
        +String username
        +String nombre
        +String apellido
        +String correo
        +CargoAdminUnidad cargoNombre
        +Integer unidadId
    }

    class AdminUnidad {
        +CargoAdminUnidad cargoNombre
        +Unidad unidad
    }

    class Usuario {
        +Integer id
        +String username
        +String nombre
        +String apellido
        +String correo
        +Rol rol
        +Boolean activo
    }

    class Unidad {
        +Integer unidadId
        +String nombre
        +String tipo
    }

    class CargoAdminUnidad {
        <<enumeration>>
        SECRETARIA_ACADEMICA
        DEPARTAMENTO_DOCENTE
        DIRECTOR_DEPARTAMENTO
    }

    class CambioService {
        -CambioRepository cambioRepository
        +registrarCambio(tabla: String, campo: String, valorAnterior: Object, valorNuevo: Object, usuario: Usuario)
    }

    class Cambio {
        +Integer id
        +String tablaAfectada
        +String campoAfectado
        +String valorAnterior
        +String valorNuevo
        +LocalDateTime fechaHora
        +Usuario usuario
    }

    Usuario <|-- AdminUnidad
    UsuarioController --> AdminUnidadService
    AdminUnidadService --> AdminUnidadUpdateRequest
    AdminUnidadService --> AdminUnidad
    AdminUnidadService --> CambioService
    AdminUnidad --> Unidad
    AdminUnidad --> CargoAdminUnidad
    CambioService --> Cambio
    Cambio --> Usuario
```

---

## 3. Component and Data Flow Architecture

Shows how data flows through the layers from the HTTP boundary to persistence and auditing.

```mermaid
graph TD
    Client["Client (Browser / Global Admin)"] -->|PUT /api/usuarios/admin-unidad/{id}| Controller["UsuarioController"]
    Controller -->|DTO Validation @Valid| RequestDTO["AdminUnidadUpdateRequest"]
    Controller -->|Delegates execution| Service["AdminUnidadService"]
    
    Service -->|Verify collision| UserRepo[("UsuarioRepository")]
    Service -->|Find Unit| UnitRepo[("UnidadRepository")]
    Service -->|Load AdminUnidad| AdminRepo[("AdminUnidadRepository")]
    
    Service -->|Audit delta per field| AuditService["CambioService"]
    AuditService -->|Persist if changed| CambioRepo[("CambioRepository")]
    
    Service -->|Persist updated user| AdminRepo
    Service -->|Return entity| Controller
    Controller -->|200 OK + JSON| Client
```
