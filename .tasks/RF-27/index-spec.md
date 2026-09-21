# RF-27 — specification index

## Source artifacts

| File | Purpose |
| ---- | ------- |
| `raw-requirement.md` | Original unstructured requirement for registering a global administrator |
| `spec-01-registrar-admin-global.md` | Formal specification in Gherkin for creating the user and sending the email |
| `open-questions.md` | Technical and functional doubts regarding the already implemented feature |
| `solution-diagrams.md` | Technical flow diagrams mapping to existing code |

## Field mapping (contract ↔ Core)

| JSON/Contract Field | DTO | Domain | Description |
| ------------------- | --- | ------ | ----------- |
| `username` | `AdminGlobalRequest.username` | `AdminGlobal.username` | The username used to log in |
| `correo` | `AdminGlobalRequest.correo` | `AdminGlobal.correo` | The email address to send the provisonal password |
| `apellido` | `AdminGlobalRequest.apellido` | `AdminGlobal.apellido` | The administrator's last name |
| `nombre` | `AdminGlobalRequest.nombre` | `AdminGlobal.nombre` | The administrator's first name |
| `cargoNombre` | `AdminGlobalRequest.cargoNombre` | `AdminGlobal.cargoNombre` | Position name within the faculty (Must be from a predefined Enum, e.g. Dirección de Personal, Despacho) |

## Execution order

| Order | Spec file | Summary | Depends on |
| ----- | --------- | ------- | ---------- |
| 1 | `spec-01-registrar-admin-global.md` | Registering Global Administrator and Email notification | None |

## Suggested increments / Backbone

1. **Walking Skeleton / MVP**: Receive data in POST, validate against DB if it exists, save to DB, return 201.
2. **Make it Releasable**: Integrate email generation and temporary password, set `activo=true` and `debeCambiarContrasenia=true`.
*(Note: all steps are already implemented)*
