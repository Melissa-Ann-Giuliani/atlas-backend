# Raw Requirements
 
## RF05: Listado de todos los docentes activos
 
| Campo | Detalle |
|---|---|
| **Descripción** | El sistema debe ser capaz de mostrar un listado de todos los docentes activos en la facultad |
| **Actor** | Administrador Global, Administrador de Unidad o Auditor |
| **Entrada** | El administrador ingresa a Listado
Selecciona “Docentes” de entre las opciones disponibles. |
| **Condición** | El administrador de unidad sólo puede visualizar información correspondiente a su departamento/instituto/centro 
Un auditor no puede modificar información, solo puede acceder a la información para lecturas
Los docentes en la lista deben tener el estado “Activo”, es decir, están asignados actualmente a un cargo
Debe ser capaz de aplicar filtros sobre la información en la tabla. |
| **Salida** | Se mostrará el listado de docentes (con filtros si lo desearan), con la posibilidad de hacer clic sobre un docente para acceder a más información (datos de contacto, designación actual junto con su cargo y funciones asociadas, etc). |