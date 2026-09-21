# Raw Requirements
 
## RF28: Registrar un nuevo administrador de unidad al sistema
 
| Campo | Detalle |
|---|---|
| **Descripción** | El sistema debe ser capaz de registrar un nuevo administrador de unidad al sistema. |
| **Actor** | Administrador Global |
| **Entrada** | El administrador ingresa a Gestión
Selecciona la opción Nuevo Usuario
Ingresa 
nombre de usuario, 
correo electrónico, 
apellido/s y nombre/s
Selecciona para Rol la opción Administrador de Unidad
Elige cuál es su cargo correspondiente dentro de la facultad (Director, Vicedirector, Secretaría Administrativa), así como su dependencia (departamento, instituto o centro) |
| **Condición** | El administrador no debe estar previamente registrado en el sistema 
El administrador automáticamente se le asigna el estado “Activo”
Al usuario se le genera automáticamente una contraseña, la cual el usuario no podrá ver en texto simple. Sólo será visible para el usuario creado. |
| **Salida** | El administrador de unidad es cargado al sistema, y figura entre la lista de usuarios de la facultad.
Se le enviará al correo un mail de confirmación, con su contraseña temporal, para que el usuario lo cambie luego dentro de su Perfil. |