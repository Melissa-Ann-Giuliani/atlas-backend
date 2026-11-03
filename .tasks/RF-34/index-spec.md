# RF-34 — Specification Index

## Source artifacts

| File | Purpose |
|------|---------|
| `raw-requirement.md` | Original requirement definition for RF-34 |
| `open-questions.md` | Decision log on domain scope, field mutability, and audit rules |
| `spec-01-modify-auditor.md` | Specification for modifying Auditor information and auditing changes |
| `solution-diagrams.md` | Sequence, class, and architectural component flow diagrams |

## Field mapping (Contract ↔ Domain)

| Request Field (`AuditorUpdateRequest`) | Entity Field (`Auditor`) | Validation / Domain Rules | Audited (`cambios.campoAfectado`) |
|---|---|---|---|
| `username` | `username` | Not blank, max 50, unique across `usuarios` (excluding current record) | `username` |
| `nombre` | `nombre` | Not blank, max 50 | `nombre` |
| `apellido` | `apellido` | Not blank, max 50 | `apellido` |
| `correo` | `correo` | Valid email format, not blank, unique across `usuarios` (excluding current record) | `correo` |
| `cargoNombre` | `cargoNombre` | Enum `CargoAuditor` (not null) | `cargo_au_nombre` |

## Execution order

| Order | Spec file | Summary | Depends on |
|-------|-----------|---------|------------|
| 1 | `spec-01-modify-auditor.md` | Updating Auditor personal, contact, and cargo info with audit logging in `cambios` | None |

## Suggested increments / Backbone

1. **DTO Creation (`AuditorUpdateRequest`):**
   - Define fields `username`, `nombre`, `apellido`, `correo`, and `cargoNombre` (`CargoAuditor`) with Jakarta Bean Validation constraints.
2. **Service Method (`AuditorService.modificarAuditor`):**
   - Authenticated user resolution for audit attribution.
   - Existence check for target `Auditor` by ID.
   - Uniqueness check for `username` and `correo` excluding target user ID.
   - Delta recording via `CambioService.registrarCambio("auditores", ...)` for each modified field.
   - Update and save `Auditor` entity in `AuditorRepository`.
3. **Controller Endpoint (`UsuarioController`):**
   - Expose `PUT /api/usuarios/auditor/{id}` secured with `@PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMINISTRADOR_GLOBAL')")`.
4. **Testing:**
   - Unit tests for `AuditorService.modificarAuditor` (success, delta auditing, collisions, not found).
   - Integration / MVC tests in `UsuarioControllerTest` verifying authorization, validation, and endpoint responses.
