# RF-02 — Specification Index

## Source artifacts

| File | Purpose |
|------|---------|
| [`raw-requirement.md`](./raw-requirement.md) | Initial raw requirements provided by the product owner. |
| [`open-questions.md`](./open-questions.md) | Blockers, risks, and minor inconsistencies to resolve before implementation. **B1 is a mandatory pre-implementation decision.** |
| [`solution-diagrams.md`](./solution-diagrams.md) | Technical diagrams: sequence, class, and state diagrams of the authentication failure flow. |

## Execution order

| Order | Spec file | Summary | Depends on |
|-------|-----------|---------|------------|
| 1 | [`spec-01-login-error.md`](./spec-01-login-error.md) | Login error message — invalid credentials and inactive account handling | B1 decision in `open-questions.md` |

## Field mapping — AuthRequest ↔ AuthService ↔ Spring Security

| Request field | AuthRequest field | Spring Security concept | DB column |
|---------------|-------------------|------------------------|-----------|
| `username` | `String username` | `UsernamePasswordAuthenticationToken.principal` | `usuarios.usuario_username` |
| `password` | `String password` | `UsernamePasswordAuthenticationToken.credentials` | `usuarios.usuario_contrasenia` |
| *(n/a)* | *(n/a)* | `UserDetails.isEnabled()` ← `activo` | `usuarios.usuario_activo` |

## Suggested increments / Backbone

```
MVP (Slice 1 — this story)
  └─ Display a login error message on invalid credentials or inactive account
       → Validates the full auth failure path end-to-end (Walking Skeleton for error flows)

Better (Slice 2 — future story)
  └─ Brute-force protection: rate limiting / account lockout after N failed attempts
       → Tracked in open-questions.md R1

Best (Slice 3 — future story)
  └─ Structured JSON error response format { "message": "..." } across all error types
       → Tracked in open-questions.md M1
```
