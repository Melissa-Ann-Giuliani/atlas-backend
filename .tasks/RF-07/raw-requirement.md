# Raw Requirements
 
## RF07: Listado de todas las licencias activas
 
| Campo | Detalle |
|---|---|
| **Descripción** | El sistema debe ser capaz de mostrar un listado de todas las licencias activas en la facultad |
| **Actor** | Administrador Global, Administrador de Unidad o Auditor |
| **Entrada** | El administrador ingresa a Listado
Selecciona “Licencias” de entre las opciones disponibles |
| **Condición** | El administrador de unidad sólo puede visualizar información correspondiente a su departamento/instituto/centro 
Un auditor no puede modificar información, solo puede acceder a la información para lecturas
Las licencias en la lista deben tener el estado “Activa”, es decir, están vinculados a un docente actualmente licencia
Debe ser capaz de aplicar filtros sobre la información en la tabla |
| **Salida** | Se mostrará el listado de licencias (con filtros si lo desearan), con la posibilidad de hacer clic sobre una licencia para acceder a más información (datos de cargo, docente, designación, etc).  |