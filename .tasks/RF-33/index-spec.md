# RF-33 — Specification Index

## Source artifacts

| File | Purpose |
|------|---------|
| `raw-requirement.md` | Original requirement definition for RF-33 |
| `open-questions.md` | Decision log on domain scope, field mutability, and audit rules |
| `spec-01-modify-admin-unidad.md` | Specification for modifying Unit Administrator information and auditing changes |
| `solution-diagrams.md` | Sequence, class, and architectural component flow diagrams |

## Field mapping (Contract ↔ Domain)

| Request Field (`AdminUnidadUpdateRequest`) | Entity Field (`AdminUnidad`) | Validation / Domain Rules | Audited (`cambios.campoAfectado`) |
|---|---|---|---|
| `username` | `username` | Not blank, max 50, unique across `usuarios` | `username` |
| `nombre` | `nombre` | Not blank, max 50 | `nombre` |
| `apellido` | `apellido` | Not blank, max 50 | `apellido` |
| `correo` | `correo` | Valid email, not blank, unique across `usuarios` | `correo` |
| `cargoNombre` | `cargoNombre` | Enum `CargoAdminUnidad` (not null) | `cargo_ad_nombre` |
| `unidadId` | `unidad` | Must match existing active `Unidad` ID | `unidad_id` |

## Execution order

| Order | Spec file | Summary | Depends on |
|-------|-----------|---------|------------|
| 1 | `spec-01-modify-admin-unidad.md` | Updating AdminUnidad personal, contact, cargo, and unit info with audit logging in `cambios` | None |

## Suggested increments / Backbone

1. **Walking Skeleton & Full Contract (Spec 01):**
   - Ensure DTO `AdminUnidadUpdateRequest` contains `username`, `nombre`, `apellido`, `correo`, `cargoNombre`, and `unidadId`.
   - Verify `AdminUnidadService.modificarAdminUnidad(id, request, responsibleUsername)` logic:
     - Existence verification for target `AdminUnidad`.
     - Existence verification for `Unidad`.
     - Collision checks for `username` and `correo` excluding target user ID.
     - Delta auditing via `CambioService.registrarCambio("admin_de_unidad", ...)` for `username`, `nombre`, `apellido`, `correo`, `cargo_ad_nombre`, `unidad_id`.
   - Expose and verify `PUT /api/usuarios/admin-unidad/{id}` in `UsuarioController` protected with `@PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMINISTRADOR_GLOBAL')")`.
   - Unit and integration tests covering controller and service layers for happy paths, not found cases, collision conflicts, and validation errors.
