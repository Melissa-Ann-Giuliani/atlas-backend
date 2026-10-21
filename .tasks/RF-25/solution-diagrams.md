# Solution Diagrams — RF-25: Modificar los datos de una materia

This document details the diagrams of the proposed solution to understand the implementation of subject modification.

---

## 1. Sequence Diagram (Mandatory)

Shows the sequence flow of modifying an existing subject via `PATCH /api/materias/{id}`, illustrating existence check, role scoping, impact verification on active cátedras, uniqueness check excluding self ID, and persistence.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Administrador (Global o Unidad)
    participant Ctrl as MateriaController
    participant Svc as MateriaService
    participant MatRepo as MateriaRepository
    participant CatRepo as CatedraRepository
    participant Sec as Security / UsuarioRepository
    participant DB as Base de Datos

    Admin->>Ctrl: PATCH /api/materias/{id} (MateriaPatchRequest)
    activate Ctrl
    Ctrl->>Svc: modificarMateria(id, patchRequest, username)
    activate Svc

    Svc->>MatRepo: findById(id)
    MatRepo->>DB: SELECT * FROM materias WHERE materia_id = ?
    DB-->>MatRepo: Materia existente / Empty
    alt Materia no encontrada
        MatRepo-->>Svc: Optional.empty()
        Svc-->>Ctrl: 404 NOT_FOUND ("Materia no encontrada")
        Ctrl-->>Admin: HTTP 404 Not Found
    end

    Svc->>Sec: Validar permisos y rol del usuario
    alt Usuario es ADMIN_UNIDAD
        opt La materia no pertenece a la unidad académica asignada al usuario
            Svc-->>Ctrl: 403 FORBIDDEN ("No tiene permisos para modificar materias en esta unidad académica")
            Ctrl-->>Admin: HTTP 403 Forbidden
        end
    end

    opt Modifica horas o despliegue
        Svc->>CatRepo: existsByMateria_IdAndEstadoIgnoreCase(id, "ACTIVA")
        CatRepo->>DB: SELECT COUNT(*) > 0 FROM catedras WHERE materia_id = ? AND catedra_estado = 'ACTIVA'
        DB-->>CatRepo: boolean
        alt Posee cátedras activas asociadas
            Svc-->>Ctrl: 409 CONFLICT / 400 BAD_REQUEST ("No se pueden modificar horas o despliegue porque la materia tiene cátedras activas asociadas")
            Ctrl-->>Admin: HTTP 409 Conflict
        end
    end

    opt Modifica nombre
        Svc->>MatRepo: existsByNombreIgnoreCaseAndUnidad_UnidadIdAndPlanEstudioAndIdNot(nombre, materia.unidad.id, materia.planEstudio, id)
        MatRepo->>DB: SELECT COUNT(*) > 0 FROM materias WHERE ... AND materia_id != ?
        DB-->>MatRepo: boolean
        alt Ya existe otra materia con mismo nombre en este plan y unidad
            Svc-->>Ctrl: 409 CONFLICT ("Ya existe otra materia con ese nombre para esta unidad y plan de estudio")
            Ctrl-->>Admin: HTTP 409 Conflict
        end
    end

    Svc->>Svc: Aplicar cambios parciales validados a la entidad Materia
    Svc->>MatRepo: save(materia)
    MatRepo->>DB: UPDATE materias SET ... WHERE materia_id = ?
    DB-->>MatRepo: Materia actualizada
    Svc-->>Ctrl: MateriaDetailDTO
    deactivate Svc
    Ctrl-->>Admin: HTTP 200 OK (MateriaDetailDTO)
    deactivate Ctrl
```

---

## 2. Class Diagram (Mandatory)

Shows the classes, interfaces, attributes, and methods involved in modifying subject data and their relationships.

```mermaid
classDiagram
    class MateriaController {
        -MateriaService materiaService
        +crearMateria(MateriaCreateRequest, Authentication) ResponseEntity~?~
        +modificarMateria(Integer, MateriaPatchRequest, Authentication) ResponseEntity~?~
    }

    class MateriaService {
        -MateriaRepository materiaRepository
        -CatedraRepository catedraRepository
        -UnidadRepository unidadRepository
        -UsuarioRepository usuarioRepository
        -AdminUnidadRepository adminUnidadRepository
        +crearMateria(MateriaCreateRequest, String) MateriaDetailDTO
        +modificarMateria(Integer, MateriaPatchRequest, String) MateriaDetailDTO
    }

    class MateriaRepository {
        <<interface>>
        +existsByNombreIgnoreCaseAndUnidad_UnidadIdAndPlanEstudio(String, Integer, Integer) boolean
        +existsByNombreIgnoreCaseAndUnidad_UnidadIdAndPlanEstudioAndIdNot(String, Integer, Integer, Integer) boolean
    }

    class CatedraRepository {
        <<interface>>
        +existsByMateria_IdAndEstadoIgnoreCase(Integer, String) boolean
    }

    class MateriaPatchRequest {
        -String nombre
        -Integer horas
        -String anio
        -String despliegue
    }

    class MateriaDetailDTO {
        -Integer id
        -String nombre
        -Integer horas
        -String anio
        -String despliegue
        -Integer planEstudio
        -Integer unidadId
        -String unidadNombre
        -Integer dependenciaId
        -String dependenciaNombre
    }

    class Materia {
        -Integer id
        -String nombre
        -Integer horas
        -String anio
        -String despliegue
        -Integer planEstudio
        -Unidad unidad
    }

    MateriaController --> MateriaService
    MateriaService --> MateriaRepository
    MateriaService --> CatedraRepository
    MateriaService ..> MateriaPatchRequest
    MateriaService ..> MateriaDetailDTO
    MateriaRepository ..> Materia
```

---

## 3. Additional Useful Diagram: Decision & Validation Flowchart

Shows the step-by-step validation logic for partial modifications (`PATCH`).

```mermaid
flowchart TD
    Start([PATCH /api/materias/{id}]) --> CheckExists{¿Materia existe en BD?}
    CheckExists -- No --> Err404[404 Not Found: Materia no encontrada]
    CheckExists -- Sí --> CheckRole{¿Usuario es ADMIN_UNIDAD?}

    CheckRole -- Sí --> CheckUnitPerm{¿Materia pertenece a su unidad asignada?}
    CheckUnitPerm -- No --> Err403[403 Forbidden: Sin permisos en esta unidad]
    CheckUnitPerm -- Sí --> CheckFields[Validar campos provistos en payload]
    CheckRole -- No (ADMIN_GLOBAL) --> CheckFields

    CheckFields --> CheckCatedras{¿Modifica horas o despliegue y tiene cátedras activas?}
    CheckCatedras -- Sí --> ErrCatedra[409 Conflict: Cátedras activas asociadas]
    CheckCatedras -- No --> CheckNameChange{¿Modifica nombre?}

    CheckNameChange -- No --> UpdateFields[Actualizar campos en memoria]
    CheckNameChange -- Sí --> CheckUniqueness{¿Existe OTRA materia con mismo nombre en este plan y unidad? id != actual}

    CheckUniqueness -- Sí --> Err409[409 Conflict: Nombre duplicado]
    CheckUniqueness -- No --> UpdateFields

    UpdateFields --> SaveDB[Guardar cambios en BD]
    SaveDB --> Success([200 OK: MateriaDetailDTO])
```
