# Open Questions — RF-27: Registrar un nuevo administrador global al sistema

## Summary (Only for Agent, do not edit this table manually)

| Category | Decision |
| -------- | -------- |
| Security | Provisional password expiry duration TBD |
| Reliability | Behavior when email fails TBD |
| Logic | Cargo enumeration/validation TBD |

## 1. Provisional Password Expiry

**Context:** The current system generates a temporary password and flags `debeCambiarContrasenia = true`. The email says "change it within 24 hs", but there is no programmatic expiration enforcement of that 24-hour limit in `AdminGlobalService`.

**Question:** Do we need to enforce the 24-hour expiration programmatically, preventing login after 24 hours if the password hasn't been changed?

- [ ] **Option A (Current):** No, the 24 hours is just a suggestion in the email. They are forced to change it on login anyway, regardless of how much time passed.
- [x] **Option B:** Yes, implement a programmatic check comparing `fechaReset` against the current time during the authentication process.
- [ ] Other: ________________

---

## 2. Email Delivery Failure Handling

**Context:** In `AdminGlobalService.registrarAdminGlobal()`, `emailService.sendWelcomeAdminGlobal()` is called after `adminGlobalRepository.save()`.

**Question:** What should happen if the SMTP server is down or the email fails to send?

- [ ] **Option A (Current/Default behavior):** If `sendWelcomeAdminGlobal` throws an exception, the `@Transactional` annotation rolls back the user creation, meaning the user is NOT saved in the database, and the caller receives an HTTP 500.
- [x] **Option B:** Catch the exception, commit the user to the database, but return a warning in the response indicating the email could not be sent (so the admin can trigger a resend manually).
- [ ] Other: ________________

---

## 3. Position (Cargo) Validation

**Context:** `cargoNombre` is currently accepted as an open `String`.

**Question:** Should `cargoNombre` be validated against a predefined list of valid faculty positions (e.g. Decano, Secretario, etc.), or is free-text acceptable?

- [ ] **Option A (Current):** Free-text is acceptable.
- [ ] **Option B:** Create a `Cargo` entity or `Enum` to enforce strict validation.
- [x] Other: My idea was that there would be a few options for each role (adming global for example, would have Dirección de Personal or Despacho)
