# Raw Requirements
 
## RF20: Baja de un cargo
 
| Campo | Detalle |
|---|---|
| **Descripción** | El sistema debe ser capaz de generar la baja de un cargo |
| **Actor** | Administrador Global |
| **Entrada** | Accede al sector Gestión 
Dentro del cuadro Cargos, selecciona Baja de Cargo
Busca el cargo por número de cargo, fecha de creación o estado  |
| **Condición** | El cargo debe existir previamente
El cargo no debe estar vinculado a una designación vigente; si lo estuviera, primero debe desvincularse o finalizarse dicha designación |
| **Salida** | Queda registrado en el sistema la baja del cargo, en estado “Dado de baja”, manteniendo su existencia y datos en el sistema.  |