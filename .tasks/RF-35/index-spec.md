# RF-35 — Specification Index

## Source artifacts

| File | Purpose |
|------|---------|
| `raw-requirement.md` | Original requirement definition for RF-35 (Baja a cualquier usuario en el sistema) |
| `open-questions.md` | Decision log on blockers, scope of active cargos, and service architecture |
| `spec-01-decommission-user.md` | Specification for user decommissioning and validation rules |
| `solution-diagrams.md` | Sequence, class, and state validation flow diagrams |

## Execution order

| Order | Spec file | Summary | Depends on |
|-------|-----------|---------|------------|
| 1 | `spec-01-decommission-user.md` | Decommissioning users with role verification, cargo checks, and audit logging | None |

## Suggested increments / Backbone

1. **Service Creation (`UsuarioService`):**
   - Create `UsuarioService` in `com.atlas.atlas_backend.usuario`.
   - Implement `darDeBajaUsuario(Integer id, String authUsername)`:
     - Check authenticated user not null and not self (`target.username != authUsername`).
     - Check existence (`404 Not Found`).
     - Check current state (`activo == true`, else `400 Bad Request`).
     - Check active cargos if user is a `Docente` (`DesignacionRepository.findByUsuarioId`), ensuring no designation is active (`409 Conflict`).
     - Update `usuario.setActivo(false)`.
     - If `Docente`, set `docente.setEstado("Inactivo")`.
     - Log changes via `CambioService.registrarCambio`.
2. **Controller Endpoint (`UsuarioController`):**
   - Expose `PATCH /api/usuarios/{id}/baja` secured with `@PreAuthorize("hasRole('ADMIN_GLOBAL') or hasRole('ADMINISTRADOR_GLOBAL')")`.
3. **Automated Testing:**
   - Unit tests in `UsuarioServiceTest` covering all scenarios (happy path, active cargos conflict, self-deactivation, already inactive, not found).
   - Controller integration tests in `UsuarioControllerTest` verifying authorization, status codes, and error responses.
