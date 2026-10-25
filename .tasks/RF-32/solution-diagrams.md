# Solution Diagrams — RF-32

This document details the diagrams of the proposed solution for modifying Global Administrators.

---

## 1. Sequence Diagram (Mandatory)

Shows the sequence flow when a Global Administrator updates another Global Administrator's profile and cargo.

```mermaid
sequenceDiagram
    autonumber
    actor AG as Global Administrator
    participant UC as UsuarioController
    participant AGS as AdminGlobalService
    participant AGR as AdminGlobalRepository
    participant UR as UsuarioRepository
    participant CS as CambioService
    participant CR as CambioRepository

    AG->>UC: PUT /api/usuarios/admin-global/{id} (payload)
    Note over UC: PreAuthorize: hasRole('ADMIN_GLOBAL')
    UC->>AGS: modificarAdminGlobal(id, request, responsibleUsername)

    AGS->>UR: findByUsername(responsibleUsername)
    UR-->>AGS: responsibleUsuario

    AGS->>AGR: findById(id)
    alt AdminGlobal not found
        AGR-->>AGS: Optional.empty()
        AGS-->>UC: throw ResponseStatusException(404, "Administrador Global no encontrado")
        UC-->>AG: 404 Not Found
    else AdminGlobal found
        AGR-->>AGS: adminGlobal

        opt Username modified
            AGS->>UR: findByUsername(newUsername)
            alt Username already used by another user
                UR-->>AGS: otherUser (id != currentId)
                AGS-->>UC: throw ResponseStatusException(409, "El nombre de usuario ya se encuentra en uso")
                UC-->>AG: 409 Conflict
            end
        end

        opt Email modified
            AGS->>UR: findByCorreo(newCorreo)
            alt Email already used by another user
                UR-->>AGS: otherUser (id != currentId)
                AGS-->>UC: throw ResponseStatusException(409, "El correo electrónico ya se encuentra en uso")
                UC-->>AG: 409 Conflict
            end
        end

        Note over AGS,CS: Audit changes via CambioService
        AGS->>CS: registrarCambio("admin_globales", "username", oldVal, newVal, responsibleUsuario)
        AGS->>CS: registrarCambio("admin_globales", "nombre", oldVal, newVal, responsibleUsuario)
        AGS->>CS: registrarCambio("admin_globales", "apellido", oldVal, newVal, responsibleUsuario)
        AGS->>CS: registrarCambio("admin_globales", "correo", oldVal, newVal, responsibleUsuario)
        AGS->>CS: registrarCambio("admin_globales", "cargo_ag_nombre", oldVal, newVal, responsibleUsuario)

        CS->>CR: save(cambio) [only if value changed]

        Note over AGS: Apply changes to entity
        AGS->>AGR: save(adminGlobal)
        AGR-->>AGS: updatedAdminGlobal

        AGS-->>UC: updatedAdminGlobal
        UC-->>AG: 200 OK (updatedAdminGlobal)
    end
```

---

## 2. Class Diagram (Mandatory)

Outlines the entities, DTOs, controllers, and services involved in RF-32.

```mermaid
classDiagram
    class UsuarioController {
        -AdminGlobalService adminGlobalService
        +modificarAdminGlobal(id: Integer, request: AdminGlobalUpdateRequest, auth: Authentication): ResponseEntity
    }

    class AdminGlobalService {
        -AdminGlobalRepository adminGlobalRepository
        -UsuarioRepository usuarioRepository
        -CambioService cambioService
        +modificarAdminGlobal(id: Integer, request: AdminGlobalUpdateRequest, username: String): AdminGlobal
    }

    class AdminGlobalUpdateRequest {
        +String username
        +String nombre
        +String apellido
        +String correo
        +CargoAdminGlobal cargoNombre
    }

    class Usuario {
        +Integer id
        +String username
        +String contrasenia
        +String correo
        +String apellido
        +String nombre
        +Boolean activo
        +Rol rol
    }

    class AdminGlobal {
        +CargoAdminGlobal cargoNombre
    }

    class CargoAdminGlobal {
        <<enumeration>>
        DIRECCION_DE_PERSONAL
        DESPACHO
    }

    class CambioService {
        -CambioRepository repository
        +registrarCambio(tabla: String, campo: String, valorAnterior: Object, valorNuevo: Object, usuario: Usuario): void
    }

    class Cambio {
        +Integer id
        +String tablaAfectada
        +String campoAfectado
        +String valorAnterior
        +String valorNuevo
        +LocalDate fechaHora
        +Usuario usuario
    }

    Usuario <|-- AdminGlobal
    AdminGlobal --> CargoAdminGlobal
    UsuarioController --> AdminGlobalService
    UsuarioController ..> AdminGlobalUpdateRequest
    AdminGlobalService --> CambioService
    AdminGlobalService ..> AdminGlobal
    Cambio --> Usuario
```

---

## 3. Data Flow and Component Architecture

```mermaid
flowchart TD
    A[HTTP PUT Request /api/usuarios/admin-global/:id] --> B{Security Check<br/>hasRole ADMIN_GLOBAL}
    B -- Unauthorized --> C[403 Forbidden / 401 Unauthorized]
    B -- Authorized --> D[Validate Request DTO]
    D -- Validation Errors --> E[400 Bad Request]
    D -- Valid --> F[AdminGlobalService]
    F --> G{Exists AdminGlobal with id?}
    G -- No --> H[404 Not Found]
    G -- Yes --> I{Username or Email changed & taken by other user?}
    I -- Yes --> J[409 Conflict]
    I -- No --> K[Record Field Deltas in CambioService]
    K --> L[Persist updated AdminGlobal]
    L --> M[200 OK Response with updated data]
```
