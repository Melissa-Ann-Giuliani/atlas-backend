# Raw Requirements
 
## RF27: Registrar un nuevo administrador global al sistema
 
| Campo | Detalle |
|---|---|
| **Descripción** | El sistema debe ser capaz de registrar un nuevo administrador global al sistema. |
| **Actor** | Administrador Global |
| **Entrada** | El administrador ingresa a Gestión
Selecciona la opción Nuevo Usuario
Ingresa 
nombre de usuario, 
correo electrónico, 
apellido/s y nombre/s
Selecciona para Rol la opción Administrador Global
Elige cuál es su cargo correspondiente dentro de la facultad |
| **Condición** | El usuario no debe estar previamente registrado en el sistema 
El usuario automáticamente se le asigna el estado “Activo”
Al usuario se le genera automáticamente una contraseña, la cual el usuario no podrá ver en texto simple. Sólo será visible para el usuario creado. |
| **Salida** | El administrador global es cargado al sistema, y figura entre la lista de usuarios de la facultad.
Se le enviará al correo un mail de confirmación, con su contraseña temporal, para que el usuario lo cambie luego dentro de su Perfil. |