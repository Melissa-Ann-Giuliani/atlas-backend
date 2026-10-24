# Solution Diagrams — RF-31

This document details the diagrams of the proposed solution for teacher information modification, taking into account the decisions resolved in `open-questions.md`:
- Dedicated endpoints per modification sector (`perfil`, `cargos`, `funciones`).
- Immutable DNI & CUIL.
- Audit history logging directly into the existing `cambios` table (`Cambio` entity / `CambioService`).
- Unit Administrator isolation (teachers outside the unit cannot be viewed or modified).

---

## 1. Sequence Diagram (Mandatory)

Shows the sequence flow for updating teacher profile information with authorization and audit trail persistence in `cambios`.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Administrador (Global / Unidad)
    participant Ctrl as DocenteController
    participant Svc as DocenteService
    participant Sec as SecurityContext
    participant DocRepo as DocenteRepository
    participant CambioSvc as CambioService
    participant CambioRepo as CambioRepository
    participant DB as Base de Datos

    Admin->>Ctrl: PUT /docentes/{id} (DocenteProfileUpdateRequest)
    Ctrl->>Sec: Obtener usuario autenticado y rol
    Ctrl->>Svc: modificarPerfilDocente(id, request, usuarioAutenticado)

    Svc->>DocRepo: findById(id)
    alt Docente no existe
        DocRepo-->>Svc: empty
        Svc-->>Ctrl: throw EntityNotFoundException (404)
        Ctrl-->>Admin: 404 Not Found
    end
    DocRepo-->>Svc: docenteActual

    alt Usuario es Admin de Unidad
        Svc->>Svc: validarPertenenciaUnidad(admin, docenteActual)
        alt Docente no pertenece a la unidad del admin
            Svc-->>Ctrl: throw AccessDeniedException (403)
            Ctrl-->>Admin: 403 Forbidden
        end
    end

    alt Intento de alterar DNI o CUIL
        Svc->>Svc: verificarInmutabilidadDniCuil(docenteActual, request)
        alt DNI o CUIL alterados
            Svc-->>Ctrl: throw IllegalArgumentException (400 DNI/CUIL inmutable)
            Ctrl-->>Admin: 400 Bad Request
        end
    end

    alt Modificó email institucional
        Svc->>DocRepo: existsByEmailInstitucionalAndIdNot(email, id)
        alt Email ya utilizado por otro usuario
            Svc-->>Ctrl: throw DuplicateKeyException (400 / 409)
            Ctrl-->>Admin: 400 Bad Request
        end
    end

    Svc->>CambioSvc: registrarCambios(docenteActual, request, usuarioAutenticado)
    loop Por cada campo modificado
        CambioSvc->>CambioRepo: save(new Cambio("docentes", campo, valorAnt, valorNuevo, fecha, usuario))
        CambioRepo->>DB: INSERT INTO cambios
    end

    Svc->>Svc: aplicarValores(docenteActual, request)
    Svc->>DocRepo: save(docenteActual)
    DocRepo->>DB: UPDATE docentes / usuarios

    Svc-->>Ctrl: DocenteDetailsDTO
    Ctrl-->>Admin: 200 OK (Docente actualizado)
```

---

## 2. Class Diagram (Mandatory)

Shows the classes, interfaces, attributes, and relationships involved in the solution using the existing `Cambio` entity.

```mermaid
classDiagram
    class DocenteController {
        -DocenteService docenteService
        +modificarPerfilDocente(Integer id, DocenteProfileUpdateRequest request, Authentication auth) ResponseEntity~DocenteDetailsDTO~
        +modificarCargoDocente(Integer docenteId, Integer cargoId, CargoUpdateRequest request, Authentication auth) ResponseEntity~Void~
        +modificarFuncionDocente(Integer docenteId, Integer funcionId, FuncionUpdateRequest request, Authentication auth) ResponseEntity~Void~
    }

    class DocenteService {
        -DocenteRepository docenteRepository
        -AdminUnidadRepository adminUnidadRepository
        -CambioService cambioService
        +modificarPerfilDocente(Integer id, DocenteProfileUpdateRequest request, String username) DocenteDetailsDTO
        -validarPertenenciaUnidad(Usuario admin, Docente docente) void
        -validarInmutabilidadDniCuil(Docente actual, DocenteProfileUpdateRequest request) void
    }

    class DocenteProfileUpdateRequest {
        +String nombre
        +String apellido
        +String tipoDni
        +String emailInstitucional
        +LocalDate fechaIngreso
        +String domicilio
        +Integer antigPrevia
        +List~String~ telefonos
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

    class CambioService {
        -CambioRepository repository
        +registrarCambio(String tabla, String campo, String anterior, String nuevo, Usuario responsable) Cambio
    }

    class CambioRepository {
        <<interface>>
        +save(Cambio cambio) Cambio
    }

    DocenteController --> DocenteService
    DocenteController ..> DocenteProfileUpdateRequest
    DocenteService --> CambioService
    CambioService --> CambioRepository
    CambioService ..> Cambio
```

---

## 3. Decision and Scope Flow (Additional Diagram)

Flow of authorization and validation for modifying a teacher profile.

```mermaid
flowchart TD
    Start([PUT /docentes/{id}]) --> Find[Buscar Docente en BD]
    Find --> Exists{¿Existe el docente?}
    Exists -- No --> Err404[404 Not Found]
    Exists -- Sí --> RoleCheck{¿Rol del solicitante?}

    RoleCheck -- Admin Unidad --> CheckUnit{¿Docente tiene cargo<br/>activo en su Unidad?}
    CheckUnit -- No --> Err403[403 Forbidden]
    CheckUnit -- Sí --> CheckImmutable[Verificar DNI y CUIL]

    RoleCheck -- Admin Global --> CheckImmutable

    CheckImmutable --> ImmutableValid{¿DNI/CUIL intactos?}
    ImmutableValid -- No --> Err400Dni[400 Bad Request: DNI/CUIL inmutables]
    ImmutableValid -- Sí --> CheckEmail{¿Cambió Email?}

    CheckEmail -- Sí --> EmailUnique{¿Email ya existe?}
    EmailUnique -- Sí --> Err400Email[400 Bad Request: Email duplicado]
    EmailUnique -- No --> TrackDeltas[Detectar campos modificados]
    CheckEmail -- No --> TrackDeltas

    TrackDeltas --> InsertCambios[Insertar registros en tabla cambios]
    InsertCambios --> UpdateDocente[Actualizar Docente y Usuario]
    UpdateDocente --> Resp200([200 OK con Perfil Actualizado])
```
