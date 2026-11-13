# Spec 01 — Asignar funciones a un cargo perteneciente a una designación

## Overview
Permite a un Administrador Global o Administrador de Unidad registrar las funciones/actividades asignadas a un cargo que forma parte del trámite de designación de un docente. Al completar la asignación, se valida que la carga horaria no exceda la dedicación del cargo ($\sum \text{horas} \le \text{dedicación}$), se valida que las materias pertenezcan a la misma unidad académica del cargo, se actualizan las horas ocupadas, se genera la documentación respaldatoria en PDF, se pasa al docente al estado `"Activo"` y se envía la notificación por correo electrónico.

---

## Endpoint
`POST /api/cargos/{cargoId}/funciones`

### Request Headers
- `Authorization: Bearer <jwt-token>` (Roles permitidos: `ADMIN_GLOBAL`, `ADMIN_UNIDAD`)

### Request Body (`AsignarFuncionesCargoRequest`)
```json
{
  "funciones": [
    {
      "nombre": "Docencia Frente a Curso",
      "tipo": "Planta",
      "horas": 10,
      "materiaId": 3
    },
    {
      "nombre": "Proyecto de Investigación Aplicada",
      "tipo": "Investigación",
      "horas": 10,
      "materiaId": 3
    }
  ]
}
```

### Success Response `200 OK` (`DesignacionCompletaResponseDTO`)
```json
{
  "cargoId": 14,
  "horasDedicacion": 20,
  "horasOcupadas": 20,
  "funciones": [
    {
      "id": 101,
      "nombre": "Docencia Frente a Curso",
      "tipo": "Planta",
      "horas": 10,
      "materia": {
        "id": 3,
        "nombre": "Algoritmos y Estructuras de Datos"
      }
    },
    {
      "id": 102,
      "nombre": "Proyecto de Investigación Aplicada",
      "tipo": "Investigación",
      "horas": 10,
      "materia": {
        "id": 3,
        "nombre": "Algoritmos y Estructuras de Datos"
      }
    }
  ],
  "designacionId": 55,
  "documentoId": 402,
  "docenteEstado": "Activo",
  "notificacionEnviada": true
}
```

---

## Business Rules & Gherkin Scenarios

### Rule 1: El cargo debe existir y estar vinculado a una designación
- El cargo debe encontrarse en estado `"Asignado"` y poseer una `designacion` asociada no nula.
- Si no existe el cargo, retorna `404 Not Found`.
- Si el cargo no está vinculado a una designación, retorna `400 Bad Request`.

```gherkin
Scenario: Intentar asignar funciones a un cargo inexistente
  Given que un administrador autenticado envía una lista de funciones
  When solicita asignar funciones al cargo con id 999999
  Then el sistema responde con estado 404 Not Found
  And el mensaje indica "El cargo especificado no existe"

Scenario: Intentar asignar funciones a un cargo sin designación vinculada
  Given un cargo con id 12 en estado "Libre" sin designación asociada
  When el administrador solicita asignar funciones al cargo 12
  Then el sistema responde con estado 400 Bad Request
  And el mensaje indica "El cargo debe estar vinculado a una designación"
```

### Rule 2: Restricción de permisos y unidad del administrador
- Un `ADMIN_UNIDAD` solo puede gestionar cargos pertenecientes a su misma unidad académica. Si intenta asignar funciones a un cargo de otra unidad, se rechaza con `403 Forbidden`.

```gherkin
Scenario: Administrador de Unidad intenta asignar funciones a un cargo de otra unidad
  Given un usuario autenticado con rol "ADMIN_UNIDAD" de la unidad "Facultad de Ingeniería"
  And un cargo perteneciente a la unidad "Facultad de Medicina"
  When el usuario intenta asignar funciones al cargo
  Then el sistema responde con estado 403 Forbidden
  And el mensaje indica "No tiene permisos para modificar cargos de otra unidad"
```

### Rule 3: Tope de horas contra la dedicación del cargo (Decisión B2 - Límite Máximo)
- La suma de las horas de todas las funciones asignadas no puede superar las horas de dedicación asignadas al cargo (`cargo.dedicacion.dedicacionHoras`).
- La suma puede ser menor o igual a la dedicación (se permite sub-ocupación parcial de horas).
- En caso de superarlo, la transacción se revierte y retorna `400 Bad Request`.

```gherkin
Scenario: La sumatoria de horas de las funciones excede la dedicación del cargo
  Given un cargo con dedicación "Simple" de 10 horas
  When el administrador envía dos funciones con 6 y 8 horas respectivamente (total 14 horas)
  Then el sistema responde con estado 400 Bad Request
  And el mensaje indica "La suma de horas (14) supera la dedicación máxima del cargo (10)"
  And no se persiste ninguna función en la base de datos

Scenario: La sumatoria de horas es menor o igual a la dedicación permitida
  Given un cargo con dedicación "Exclusiva" de 40 horas
  When el administrador envía funciones que totalizan 25 horas
  Then el sistema responde con estado 200 OK
  And las funciones se persisten correctamente
  And el cargo actualiza sus horas ocupadas a 25
```

### Rule 4: Materias válidas y pertenencia a la misma unidad académica (Decisión R3)
- Cada función debe tener un `tipo` (ej. "Planta", "Investigación", "Gestión", "Extensión") y hacer referencia a una `materiaId` existente.
- La materia indicada debe pertenecer a la misma Unidad Académica del cargo.
- Si una materia no existe, retorna `404 Not Found`.
- Si una materia pertenece a una unidad académica diferente a la del cargo, retorna `400 Bad Request`.

```gherkin
Scenario: Asignar función con una materia inexistente
  Given un cargo de 20 horas de la unidad "Facultad de Ingeniería"
  When el administrador envía una función con materiaId 8888 inexistente
  Then el sistema responde con estado 404 Not Found
  And el mensaje indica "La materia especificada no existe"

Scenario: Asignar función con una materia de otra unidad académica
  Given un cargo perteneciente a la unidad "Facultad de Ingeniería"
  And una materia existente perteneciente a la unidad "Facultad de Ciencias Económicas"
  When el administrador envía una función asignada a dicha materia
  Then el sistema responde con estado 400 Bad Request
  And el mensaje indica "La materia no pertenece a la misma unidad académica del cargo"
```

### Rule 5: Generación documental, activación del docente y notificación por correo (Decisiones R1 y R2)
- Al completarse exitosamente la asignación de funciones:
  1. Se persisten las funciones en `funciones` con su columna `funcion_tipo`.
  2. `cargo.horasOcupadas` se actualiza con la sumatoria de las horas de las funciones.
  3. Se genera y persiste un PDF oficial de la designación en `archivos` (asociado al trámite de designación).
  4. El docente asociado a la designación pasa a estado `"Activo"` (`usuario.activo = true`).
  5. Se despacha el correo electrónico de notificación al docente informando su alta activa, cargo y funciones.

```gherkin
Scenario: Asignación exitosa de funciones y finalización de designación
  Given una designación vinculada a un cargo de dedicación "Semiexclusiva" (20 horas) de "Facultad de Ingeniería"
  And un docente en estado "Inactivo" con email "docente@universidad.edu"
  When el administrador envía 2 funciones válidas (una de tipo "Planta" por 10 hs y otra de "Investigación" por 10 hs)
  Then el sistema responde con estado 200 OK
  And las 2 funciones quedan registradas asociadas al cargo con sus tipos respectivos
  And el cargo actualiza sus horas ocupadas a 20
  And se genera el archivo PDF oficial de la designación registrado en el repositorio de archivos
  And el docente pasa a tener estado "Activo"
  And se envía un correo electrónico a "docente@universidad.edu" notificando la confirmación de la designación
```
