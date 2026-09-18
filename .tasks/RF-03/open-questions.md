# Open Questions — RF-03

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01 & spec-02: Provisional password delivery mechanism

**Context:** Both specs require communicating the provisional password to the user after a reset. The raw requirement does not specify the delivery channel. If email is required, an SMTP integration must be built (a non-trivial dependency that affects sprint scope). If it is displayed on screen, no new infrastructure is needed — but the user must be physically present at the reset UI. Leaving this undefined blocks both specs from being fully implemented.

**Options:**

- [ ] **A)** Display the provisional password directly on screen (simpler, no SMTP needed; user must be at the recovery screen)
- [x] **B)** Send the provisional password via email to the registered `correo` (requires SMTP/email service integration — adds scope)
- [ ] **C)** Both: display on screen AND send via email
- [ ] **D)** Only the admin receives the provisional password (for self-service) and communicates it manually to the user out-of-band

- [ ] **Other:** ______

**Notes:**

---

### B2 — spec-01: Forced password change on next login

**Context:** The raw requirement states the user receives a "contraseña provisoria para que pueda cambiarla a una nueva." This implies the system should enforce the change. If this is required, a new column (e.g., `usuario_debe_cambiar_contrasenia BOOLEAN`) must be added to the `usuarios` table and a new interception step must be added to the login flow. Without this decision, the specs cannot define whether the `Usuario` entity needs to be extended and whether the login endpoint (RF-01) needs a follow-up story.

**Options:**

- [ ] **A)** Yes — enforce forced change on next login. Add `usuario_debe_cambiar_contrasenia` column to `usuarios`. Login flow intercepts and redirects to a password change screen before issuing the JWT.
- [ ] **B)** No — the system issues the provisional password and trusts the user to change it voluntarily. No new DB column or login interception needed.
- [x] **C)** Enforce a time-based expiry instead (e.g., provisional password expires after 24h), without blocking the login flow.

- [ ] **Other:** ______

**Notes:**

---

### B3 — spec-01 & spec-02: Provisional password generation strategy

**Context:** The raw requirement says the system "otorga" (grants) a provisional password, implying backend generation. However, an alternative is to let the user choose their new password directly during the reset flow (more UX-friendly but changes the flow from "provisional + forced change" to "direct reset"). The generation strategy impacts the DTO design, endpoint contract, and the need for a provisional delivery channel.

**Options:**

- [ ] **A)** Backend generates a random provisional password (e.g., 12-character alphanumeric) — consistent with raw requirement wording
- [ ] **B)** User chooses their new password directly in the reset form (request body includes `nuevaContrasenia`; no provisional delivery needed)
- [x] **C)** Two-step flow: system generates a reset token (sent via email), and the user follows the link to set their own new password

- [ ] **Other:** ______

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-02: Admin reset endpoint location

**Context:** spec-02 requires a `POST` endpoint to trigger an admin reset by `userId`. This could live in `AuthController` (alongside login/register) or in a new/existing `UsuarioController` (where user management endpoints naturally reside). Choosing `AuthController` keeps auth logic together; choosing `UsuarioController` better aligns with REST resource ownership (`/api/usuarios/{id}/reset-password`). The choice affects how `SecurityConfig` is updated and which controller the frontend calls.

**Options:**

- [ ] **A)** Add to `AuthController` as `POST /api/auth/admin-reset-password/{userId}` — keeps all auth-related actions in one place
- [x] **B)** Add to a new or existing `UsuarioController` as `POST /api/usuarios/{userId}/reset-password` — better REST semantics
- [ ] **C)** Create a dedicated `PasswordResetController` to isolate all reset logic (self-service + admin) from both `AuthController` and `UsuarioController`

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec | Impact | Short description | Decision |
|----|------|--------|-------------------|----------|
| B1 | spec-01 & spec-02 | Blocker | Provisional password delivery mechanism | ___ |
| B2 | spec-01 | Blocker | Forced password change on next login — new DB column? | ___ |
| B3 | spec-01 & spec-02 | Blocker | Provisional password generation strategy (backend random vs. user-chosen) | ___ |
| R1 | spec-02 | Risk | Admin reset endpoint location (AuthController vs. UsuarioController vs. new) | ___ |
