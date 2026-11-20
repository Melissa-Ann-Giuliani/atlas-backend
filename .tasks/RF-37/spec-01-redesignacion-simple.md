# spec-01: Re-designación de docente con designación próxima a vencer

## Overview
Implementar el endpoint y la lógica de negocio para la re-designación ágil de un docente cuya designación actual se encuentra próxima a vencer (o activa) desde la sección *Gestión > Vencimientos*. 

Al accionar la renovación, el sistema permite generar una **nueva designación** vinculando automáticamente al mismo docente y al cargo correspondiente (con sus funciones asociadas pre-cargadas o editables), registrando las nuevas fechas de vigencia y número de resolución oficial (Decisión B1 y B2). La designación previa pasa inmediatamente a estado `"Renovada"` (o inactiva), asegurando que la alerta de vencimiento de RF-36 quede desactivada de forma automática y se preserve la trazabilidad histórica. 

La operación valida el control de acceso multi-tenant: los usuarios con rol `ADMIN_UNIDAD` únicamente pueden re-designar docentes sobre cargos pertenecientes a su unidad académica (Decisión R2).

---

## User Story

**Como** Administrador Global o Administrador de Unidad  
**Quiero** realizar la re-designación de un docente desde el listado de vencimientos de forma simple con los datos precargados  
**Para** renovar el período de nombramiento docente sin tener que volver a ingresar manualmente todos los datos ni duplicar información administrativa, asegurando que la alerta de vencimiento cese oportunamente.

---

## Contract & Endpoints

### Endpoint: `POST /api/designaciones/{id}/redesignar`

Donde `{id}` corresponde al `designacionId` de la designación por vencer que se va a renovar.

#### Authorization:
- `hasRole('ADMIN_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD')`
- `hasRole('ADMIN_GLOBAL') or hasRole('ADMINISTRADOR_GLOBAL') or hasRole('ADMIN_UNIDAD') or hasRole('ADMIN_DE_UNIDAD') or hasRole('ADMINISTRADOR_DE_UNIDAD')`
- Validación de Unidad: Si es `ADMIN_UNIDAD`, todos los cargos de la designación a renovar deben pertenecer a la unidad del administrador autenticado.

#### Request Headers:
- `Authorization: Bearer <jwt-token>`
- `Content-Type: application/json`

#### Request Payload (`DesignacionRedesignarRequest`):
```json
{
  "nroResolucion": "RES-2026-890",
  "fechaInicio": "2026-12-01",
  "fechaFin": "2027-11-30",
  "cargoId": 12,
  "funciones": [
    {
      "materiaId": 5,
      "nombre": "Docencia Frente a Alumnos",
      "tipo": "DOCENCIA",
      "horas": 10
    }
  ]
}
```

*Reglas de Payload:*
- `nroResolucion`: Obligatorio (`@NotBlank`). No debe existir previamente para el mismo docente.
- `fechaInicio`: Obligatorio (`@NotNull`).
- `fechaFin`: Opcional o posterior a `fechaInicio`. Si se define, `fechaFin >= fechaInicio`.
- `cargoId`: Opcional. Si no se especifica, toma por defecto el cargo asociado a la designación previa.
- `funciones`: Opcional. Si se provee, reemplaza o asigna las funciones del cargo en la nueva designación. Si no se provee, se clonan o mantienen las funciones actuales del cargo.

#### Responses:

- **`201 Created`**: Retorna el DTO de la nueva designación generada.
- **`201 Created`**: Retorna el DTO (`DesignacionResponseDTO`) de la nueva designación generada, incluyendo `documentoId` (si se generó el PDF oficial de la resolución) y los resúmenes de docente y cargo.
```json
{
  "id": 185,
  "designacionId": 185,
  "documentoId": 42,
  "nroResolucion": "RES-2026-890",
  "fechaInicio": "2026-12-01",
  "fechaFin": "2027-11-30",
  "estado": "Activa",
  "docenteId": 45,
  "cargoId": 12
}
```

- **`400 Bad Request`**:
  - `fechaFin` anterior a `fechaInicio`.
  - Número de resolución ya registrado para el docente.
  - La designación previa no se encuentra en estado `"Activa"`.
- **`403 Forbidden`**: Si el `ADMIN_UNIDAD` intenta renovar una designación cuyos cargos pertenecen a otra unidad académica.
- **`404 Not Found`**: Si la designación origen `{id}`, el docente o el cargo no existen.

---

## Acceptance Criteria (Gherkin Scenarios)

### Scenario 1: Re-designación exitosa por Administrador de Unidad de su propia facultad
```gherkin
Given que existe una designación activa id=10 para el docente con id=45 en el cargo id=12
And el cargo id=12 pertenece a la unidad académica "Facultad de Ingeniería"
And el usuario "admin_ing" tiene rol "ADMIN_UNIDAD" en la "Facultad de Ingeniería"
When "admin_ing" envía una solicitud POST a "/api/designaciones/10/redesignar" con:
  | nroResolucion | "RES-2026-100" |
  | fechaInicio   | "2026-12-01"   |
  | fechaFin      | "2027-11-30"   |
Then la respuesta tiene status 201 Created
And se crea un nuevo registro Designacion con estado "Activa" y nroResolucion "RES-2026-100"
And la designación original id=10 actualiza su estado a "Renovada"
And la designación original ya no aparece en el listado de alertas de vencimientos de RF-36
```

### Scenario 2: Administrador de Unidad intenta renovar designación de otra unidad (Multi-tenant check)
```gherkin
Given que existe una designación id=20 cuyo cargo pertenece a la "Facultad de Medicina"
And el usuario autenticado "admin_ing" es Administrador de la "Facultad de Ingeniería"
When envía una solicitud POST a "/api/designaciones/20/redesignar"
Then la respuesta tiene status 403 Forbidden
And el mensaje de error indica que no tiene permisos sobre la unidad del cargo
```

### Scenario 3: Re-designación con número de resolución duplicado para el mismo docente
```gherkin
Given que el docente id=45 ya tiene registrada una designación con resolución "RES-2024-500"
When el usuario envía una solicitud POST a "/api/designaciones/10/redesignar" con nroResolucion "RES-2024-500"
Then la respuesta tiene status 400 Bad Request
And el mensaje de error indica que el docente ya posee una designación con esa resolución
```

### Scenario 4: Re-designación sobre una designación que ya no está Activa
```gherkin
Given que la designación id=10 ya tiene estado "Renovada" o "Inactiva"
When el usuario envía una solicitud POST a "/api/designaciones/10/redesignar"
Then la respuesta tiene status 400 Bad Request
And el mensaje de error indica que solo se pueden re-designar designaciones en estado Activa
```
