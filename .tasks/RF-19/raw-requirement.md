# Raw Requirements
 
## RF19: Subdividir un cargo en 2 o más cargos
 
| Campo | Detalle |
|---|---|
| **Descripción** | El sistema debe ser capaz de subdividir un cargo en 2 o más cargos |
| **Actor** | Administrador Global o Administrador de Unidad |
| **Entrada** | Accede a el sector Gestión 
Dentro del cuadro Cargos, selecciona Modificación de cargo
Busca el cargo por número de cargo, fecha de creación, estado o docente asignado
Selecciona la opción de División de cargo, indicando en cuántos subcargos se divide
Por cada subcargo, se completan los datos pertinentes al cargo: número de cargo, fecha creación, cargo asignado, carácter, categoría, dedicación, unidad, número de predecesor posible y número de origen de división, colocando allí el código del cargo padre |
| **Condición** | El cargo padre debe existir previamente
La suma de horas/dedicación de los subcargos generados no puede superar la del cargo original |
| **Salida** | Queda registrado en el sistema la división del cargo: se generan nuevos sobrecargos indicados, cada uno indicando su cargo origen, manteniendo sus datos históricos en el sistema.  |