# spec-01: Listado de designaciones próximas a vencer en Gestión > Vencimientos

## Overview
Proveer un endpoint y vista de gestión para consultar el listado detallado de todas las designaciones que se encuentran próximas a expirar dentro del umbral estático de 30 días fijado por el sistema (Decisión B1 - Opción B), con capacidad de filtrar/consultar también designaciones vencidas recientes (Decisión B2 - Opción B). La consulta filtra únicamente designaciones en estado `"Activa"` con fecha de finalización definida (Decisión R2 - Opción A) y respeta estrictamente el control de acceso basado en roles (Administrador Global vs Administrador de Unidad). La información devuelta incluye la estructura completa y enriquecida de docente, cargos y unidad (Decisión M1 - Opción A).

---

## User Story

**Como** Administrador Global o Administrador de Unidad  
**Quiero** ingresar a la sección *Gestión > Vencimientos* y consultar las designaciones con fecha de finalización cercana a vencer (y alternar opcionalmente para revisar vencidas recientes)  
**Para** anticipar los vencimientos de los docentes, planificar renovaciones o nuevos llamados a concurso sin discontinuar las actividades académicas.

---

## Contract & Endpoints

### Endpoint: `GET /api/designaciones/vencimientos`

#### Request Query Parameters:
| Parámetro | Tipo | Obligatorio | Default | Descripción |
|---|---|---|---|---|
| `incluirVencidas` | Boolean | No | `false` | Si es `false`, retorna próximas a vencer (`CURRENT_DATE <= fechaFin <= CURRENT_DATE + 30`). Si es `true`, incluye o lista también designaciones vencidas recientes (`fechaFin < CURRENT_DATE`). (Decisión B2) |
| `unidadId` | Integer | No | `null` | Filtro opcional por Unidad Académica (solo permitido para Administrador Global). Si el usuario es Administrador de Unidad, se ignora o valida contra su propia unidad. |

*Nota:* El umbral de próximos vencimientos es fijo y estático en **30 días** según la Decisión B1.

#### Responses:
- **`200 OK`**: Retorna la lista de designaciones próximas a vencer (o vencidas si `incluirVencidas=true`) ordenadas ascendentemente por `diasRestantes`.
- **`401 Unauthorized`**: Usuario no autenticado.
- **`403 Forbidden`**: Si un Administrador de Unidad intenta especificar un `unidadId` ajeno a su unidad asignada.

#### Payload de Respuesta (`List<DesignacionVencimientoDTO>`):
```json
[
  {
    "designacionId": 142,
    "numeroResolucion": "RES-2024-510",
    "fechaInicio": "2024-03-01",
    "fechaFin": "2026-11-30",
    "diasRestantes": 14,
    "estadoActual": "Activa",
    "docente": {
      "docenteId": 45,
      "nombre": "Juan",
      "apellido": "Pérez",
      "documento": "28123456",
      "legajo": "DOC-9821"
    },
    "unidad": {
      "unidadId": 2,
      "nombre": "Facultad de Ingeniería"
    },
    "cargos": [
      {
        "cargoId": 88,
        "codigo": 100234,
        "categoria": "Profesor Adjunto",
        "dedicacion": "Simple (10 hs)",
        "caracter": "Interino"
      }
    ]
  }
]
```

---

## Scenarios (Acceptance Criteria in Gherkin)

### Scenario 1: Administrador Global consulta los próximos vencimientos con umbral estático de 30 días
```gherkin
Given que el usuario está autenticado con rol "ADMINISTRADOR_GLOBAL"
And la fecha actual del sistema es "2026-11-16"
And existen designaciones en estado "Activa":
  | resolucion    | unidadId | fechaFin     | diasDesdeHoy |
  | RES-GLOBAL-01 | 1        | 2026-11-26   | 10           |
  | RES-GLOBAL-02 | 2        | 2026-12-01   | 15           |
  | RES-GLOBAL-03 | 1        | 2026-12-25   | 39           |
When realiza una petición GET a "/api/designaciones/vencimientos"
Then el sistema responde con status 200 OK
And la respuesta contiene 2 designaciones ("RES-GLOBAL-01", "RES-GLOBAL-02")
And la designación "RES-GLOBAL-01" tiene diasRestantes igual a 10
And "RES-GLOBAL-03" no es incluida por superar el umbral fijo de 30 días
And la lista está ordenada ascendentemente por diasRestantes
```

### Scenario 2: Administrador de Unidad solamente visualiza designaciones de su propia unidad
```gherkin
Given que el usuario está autenticado con rol "ADMIN_UNIDAD" asignado a la unidad ID 2 ("Facultad de Ingeniería")
And existen designaciones activas que vencen en los próximos 30 días:
  | resolucion   | unidadId | unidadNombre             | fechaFin   |
  | RES-ING-01   | 2        | Facultad de Ingeniería   | 2026-11-30 |
  | RES-MED-01   | 3        | Facultad de Medicina     | 2026-11-28 |
When realiza una petición GET a "/api/designaciones/vencimientos"
Then el sistema responde con status 200 OK
And la respuesta incluye únicamente la designación "RES-ING-01"
And no incluye ninguna designación perteneciente a la unidad 3
```

### Scenario 3: Exclusión de designaciones sin fecha de fin o con estado distinto de "Activa"
```gherkin
Given que existen designaciones registradas:
  | resolucion  | fechaFin   | estadoActual |
  | RES-PERM-01 | null       | Activa       |
  | RES-BAJA-01 | 2026-11-20 | Baja         |
  | RES-REV-01  | 2026-11-22 | Revocada     |
  | RES-OK-01   | 2026-11-25 | Activa       |
When un administrador consulta "/api/designaciones/vencimientos"
Then la respuesta solo contiene "RES-OK-01"
And "RES-PERM-01", "RES-BAJA-01" y "RES-REV-01" son excluidas del resultado
```

### Scenario 4: Consulta de designaciones ya vencidas mediante incluirVencidas=true
```gherkin
Given que la fecha actual es "2026-11-16"
And existen designaciones activas:
  | resolucion  | fechaFin   |
  | RES-VENC-01 | 2026-11-10 |
  | RES-PROX-01 | 2026-11-20 |
When el usuario consulta "GET /api/designaciones/vencimientos?incluirVencidas=true"
Then el sistema responde con status 200 OK
And la respuesta incluye a "RES-VENC-01" con diasRestantes negativo (-6)
And incluye a "RES-PROX-01" con diasRestantes positivo (4)
```

### Scenario 5: Administrador de Unidad intenta consultar designaciones de otra unidad
```gherkin
Given que el usuario está autenticado con rol "ADMIN_UNIDAD" asignado a la unidad 2
When realiza una petición GET a "/api/designaciones/vencimientos?unidadId=3"
Then el sistema rechaza la petición con status 403 Forbidden
```
