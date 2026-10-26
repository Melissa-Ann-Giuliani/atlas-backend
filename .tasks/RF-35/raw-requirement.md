# Raw Requirements
 
## RF35: Baja a cualquier usuario en el sistema
 
| Campo | Detalle |
|---|---|
| **Descripción** | El sistema debe ser capaz de dar de baja a cualquier usuario en el sistema. |
| **Actor** | Administrador Global |
| **Entrada** | El administrador ingresa a Gestión
Selecciona la opción Usuario, y selecciona Baja de Usuario
Busca al usuario a dar de baja por algún dato identificatorio (nombre de usuario, nombre o apellido, dni, etc)
Confirma la baja del usuario |
| **Condición** | El usuario debe estar previamente registrado en el sistema 
El usuario debe tener su atributo de estado en “Activo”
El usuario a dar de baja no puede tener cargos vigentes  |
| **Salida** | El usuario es dado de baja, con su estado pasando a “Inactivo”, sin posibilidad de entrar al sistema con su nombre de usuario y contraseña. 
Los datos del usuario no se eliminan del sistema, tanto para mantener datos históricos, como por si en el futuro se deba reintegrar al sistema. |