# RF-32 — Specification Index

## Source artifacts

| File | Purpose |
|------|---------|
| `raw-requirement.md` | Original requirement definition for RF-32 |
| `open-questions.md` | Decision log on domain scope, field mutability, and audit rules |
| `spec-01-modify-global-admin.md` | Specification for modifying Global Administrator information and auditing changes |
| `solution-diagrams.md` | Sequence, class, and architectural flow diagrams |

## Execution order

| Order | Spec file | Summary | Depends on |
|-------|-----------|---------|------------|
| 1 | `spec-01-modify-global-admin.md` | Updating AdminGlobal personal & contact info and cargo with audit logging in `cambios` | None |

## Suggested increments / Backbone

1. **Walking Skeleton (Spec 01):**
   - Create request DTO `AdminGlobalUpdateRequest` (`username`, `nombre`, `apellido`, `correo`, `cargoNombre`).
   - Implement service method in `AdminGlobalService.modificarAdminGlobal(id, request, responsibleUsername)`.
   - Validate existence of target `AdminGlobal`, uniqueness of new username and new email (excluding current user ID).
   - Log each field difference (`username`, `nombre`, `apellido`, `correo`, `cargo_ag_nombre`) via `CambioService.registrarCambio(...)` to the `cambios` table.
   - Expose endpoint `PUT /api/usuarios/admin-global/{id}` in `UsuarioController` protected with `@PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMINISTRADOR_GLOBAL')")`.
   - Unit and integration tests for controller and service layers.
