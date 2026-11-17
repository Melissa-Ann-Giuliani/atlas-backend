# spec-02: Parámetros opcionales de flexibilidad temporal en consulta de vencimientos

## Overview
Para cumplir con los requerimientos operativos de flexibilidad temporal definidos en la Decisión R1 (Opción B y notas añadidas en `open-questions.md`), se actualiza el endpoint `GET /api/designaciones/vencimientos` y la lógica interna de `DesignacionService` y `DesignacionRepository`.

Actualmente el servicio tiene fijado `UMBRAL_VENCIMIENTO_DIAS = 30` de forma estática (`fechaHasta = hoy.plusDays(30)` y filtro en bucle `diasRestantes > UMBRAL_VENCIMIENTO_DIAS`). Esta especificación detalla el soporte para:
1. Umbral parametrizable `@RequestParam(required = false) Integer dias` (o `diasUmbral`).
2. Indicador `@RequestParam(required = false) Boolean sinLimite`.

Si se envía un valor personalizado en `dias` (por ejemplo 60 o 90 días), o si `sinLimite = true`, el repositorio y el servicio omiten o ajustan la cota superior `fechaHasta`, permitiendo al usuario listar todas las designaciones activas pasibles de re-designación según su necesidad.

---

## User Story

**Como** Administrador Global o Administrador de Unidad  
**Quiero** consultar designaciones con un umbral de días personalizado (`dias`) o sin cota de fecha superior (`sinLimite=true`)  
**Para** identificar y gestionar de forma proactiva re-designaciones y trámites con un margen mayor a 30 días o sobre la totalidad de designaciones activas.

---

## Contract & Endpoints

### Endpoint: `GET /api/designaciones/vencimientos`

#### Request Query Parameters:
| Parámetro | Tipo | Obligatorio | Default | Descripción |
|---|---|---|---|---|
| `incluirVencidas` | `Boolean` | No | `false` | Si es `false`, omite registros donde `fechaFin < CURRENT_DATE`. Si es `true`, lista también vencidas anteriores. |
| `unidadId` | `Integer` | No | `null` | Filtrado por unidad académica (restringido para `ADMIN_UNIDAD` a su propia unidad). |
| `dias` | `Integer` | No | `null` (o `30`) | Ventana temporal personalizada de días hacia adelante. Si se define (ej. `dias=60`), evalúa hasta `hoy.plusDays(dias)`. |
| `sinLimite` | `Boolean` | No | `false` | Si es `true`, anula el filtro superior de fecha fin (`fechaHasta = null`), devolviendo todas las designaciones activas. Si `sinLimite = true`, se desestima el valor de `dias`. |

#### Lógica en Backend (`DesignacionService` y `DesignacionRepository`):

1. **Cálculo de `fechaHasta`**:
   ```java
   LocalDate fechaHasta = null;
   if (Boolean.TRUE.equals(sinLimite)) {
       fechaHasta = null; // Sin cota superior
   } else if (dias != null && dias > 0) {
       fechaHasta = hoy.plusDays(dias);
   } else {
       fechaHasta = hoy.plusDays(UMBRAL_VENCIMIENTO_DIAS); // Default 30 días
   }
   ```
2. **Consulta en Repositorio**:
   - Tanto `findProximosVencimientos` como `findVencimientosIncluyendoVencidas` ya poseen la cláusula JPA:
     `(:fechaHasta IS NULL OR d.designacionFechaFin <= :fechaHasta)`
   - Al recibir `fechaHasta = null` (cuando `sinLimite = true`), la query JPA de forma nativa omite el límite superior.
3. **Filtro de pos-procesamiento en memoria**:
   - En el bucle de `DesignacionService`:
     ```java
     if (!Boolean.TRUE.equals(sinLimite)) {
         int maxDias = (dias != null && dias > 0) ? dias : UMBRAL_VENCIMIENTO_DIAS;
         if (diasRestantes > maxDias) {
             continue;
         }
     }
     ```

---

## Acceptance Criteria (Gherkin Scenarios)

### Scenario 1: Consulta por defecto sin parámetros de fecha (Retrocompatibilidad 30 días)
```gherkin
Given que existen 3 designaciones activas:
  | id | fechaFin   | diasRestantes |
  | 1  | hoy + 10d  | 10            |
  | 2  | hoy + 25d  | 25            |
  | 3  | hoy + 45d  | 45            |
When el usuario envía GET "/api/designaciones/vencimientos"
Then la respuesta tiene status 200 OK
And retorna las designaciones con id=1 y id=2
And la designación con id=3 queda excluida por superar los 30 días
```

### Scenario 2: Consulta con umbral extendido a 60 días (`dias=60`)
```gherkin
Given que existen designaciones con vencimiento a 10, 25 y 45 días
When el usuario envía GET "/api/designaciones/vencimientos?dias=60"
Then la respuesta tiene status 200 OK
And retorna las 3 designaciones (id=1, id=2 y id=3)
```

### Scenario 3: Consulta sin límite de vencimiento (`sinLimite=true`)
```gherkin
Given que existen designaciones con vencimiento en 15 días, 90 días y 365 días
When el usuario envía GET "/api/designaciones/vencimientos?sinLimite=true"
Then la respuesta tiene status 200 OK
And retorna todas las designaciones activas independientemente de su fecha de fin
And la ordenación ascendente por diasRestantes se mantiene
```
