# Raw Requirements
 
## RF06: Listado de todos los cargos activos
 
| Campo | Detalle |
|---|---|
| **Descripción** | El sistema debe ser capaz de mostrar un listado de todos los cargos activos en la facultad |
| **Actor** | Administrador Global, Administrador de Unidad o Auditor |
| **Entrada** | El administrador ingresa a Listado
Selecciona “Cargos” de entre las opciones disponibles. |
| **Condición** | El administrador de unidad sólo puede visualizar información correspondiente a su departamento/instituto/centro 
Un auditor no puede modificar información, solo puede acceder a la información para lecturas
Los cargos en la lista deben tener el estado “Asignado”, es decir, están vinculados a un docente 
Debe ser capaz de aplicar filtros sobre la información en la tabla. |
| **Salida** | Se mostrará el listado de cargos(con filtros si lo desearan), con la posibilidad de hacer clic sobre un cargo para acceder a más información (datos de cargo, docente, designación, etc). |