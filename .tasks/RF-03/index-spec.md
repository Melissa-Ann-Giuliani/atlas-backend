# RF-03 — Specification Index

## Source Artifacts

| File | Purpose |
|------|---------|
| `raw-requirement.md` | Original raw requirements definition |
| `spec-01-password-reset-self-service.md` | User self-service password recovery (MVP / Walking Skeleton) |
| `spec-02-password-reset-admin.md` | Admin-triggered password reset from Gestión → Modificar Usuario |
| `open-questions.md` | Blockers and risks requiring PO/team decision before implementation |
| `solution-diagrams.md` | Mandatory technical diagrams: sequence, class, state |

## Field Mapping (Contract ↔ Core)

| JSON Field (Request) | DTO / Service | Domain (Usuario) |
|---|---|---|
| `username` | `PasswordResetRequest.username` | `Usuario.username` |
| `correo` | `PasswordResetRequest.correo` | `Usuario.correo` |
| *(generated)* | `AuthService.resetPassword()` | `Usuario.contrasenia` (BCrypt hash) |
| *(pending B2)* | `AuthService.resetPassword()` | `Usuario.debeCambiarContrasenia` (new flag — pending) |

## Execution Order

| Order | Spec File | Summary | Depends On |
|-------|-----------|---------|------------|
| 1 | `spec-01-password-reset-self-service.md` | Self-service: user provides username + email, receives provisional password | RF-01 (login) must be functional |
| 2 | `spec-02-password-reset-admin.md` | Admin resets a user's password from the management screen | spec-01 (provisional password generation logic must exist) |

## Suggested Increments / Backbone

```
[Backbone left → right]
Forgot password → Verify identity (username + email) → Issue provisional password → Force change on next login

[Slicing top → bottom under "Issue provisional password"]
  Slice 1 (Walking Skeleton — spec-01): Backend generates random provisional, hashes it, stores it; response confirms success.
  Slice 2 (Make it better — delivery): Provisional password sent to user via email (pending B1 resolution).
  Slice 3 (Admin path — spec-02): Admin resets via Gestión → Modificar Usuario, system auto-generates provisional.
  Slice 4 (Forced change — pending B2): System flags account, intercepts login, forces password change before granting access.
```
