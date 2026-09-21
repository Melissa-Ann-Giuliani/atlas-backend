# Raw Requirements
 
## RF29: Registrar un nuevo auditor al sistema
 
| Campo | Detalle |
|---|---|
| **Descripción** | El sistema debe ser capaz de registrar un nuevo auditor al sistema. |
| **Actor** | Administrador Global |
| **Entrada** | El administrador ingresa a Gestión
Selecciona la opción Nuevo Usuario
Ingresa: 
Nombre de usuario, 
correo electrónico, 
apellidos y nombres del nuevo usuario
Selecciona para Rol la opción Auditor
Elige cuál es su cargo correspondiente dentro de la facultad |
| **Condición** | El auditor no debe estar previamente registrado en el sistema 
El auditor automáticamente se le asigna el estado “Activo”
Al auditor se le genera automáticamente una contraseña, la cual el usuario no podrá ver en texto simple. Sólo será visible para el usuario creado. |
| **Salida** | El auditor es cargado al sistema, y figura entre la lista de usuarios de la facultad. 
Se le enviará al correo un mail de confirmación, con su contraseña temporal, para que el usuario lo cambie luego dentro de su Perfil. |