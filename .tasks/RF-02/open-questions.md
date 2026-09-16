# Open Questions Document — RF-02

Document for resolving blockers, risks, and inconsistencies detected across specs.
Simply mark the chosen option with `[x]` (or fill in `Other:`).
The agent will automatically fill in the final Summary table and update the corresponding spec files.

---

## Blockers — must be resolved before implementation

---

### B1 — spec-01: Security Risk — Username Enumeration

**Context:** The raw requirement states that the system should indicate exactly which data (username or password) is incorrect. This is a known security vulnerability called **username enumeration**: it allows attackers to probe whether a given username exists in the system by observing different error messages. The current backend (`AuthService.java`) already returns a single generic message `"Invalid username or password"` for all credential failures, which is the secure approach. Choosing Option B would require reverting this behaviour and introducing new logic to distinguish user-not-found from wrong-password cases.

**Options:**

- [x] **A)** Use a generic error message `"Invalid username or password"` for all failure cases. Spec is written to match current backend behaviour. *(Recommended for security — OWASP standard)*
- [ ] **B)** Keep the raw requirement as-is and show a specific error message indicating which field is wrong (e.g., `"Username not found"` or `"Incorrect password"`). Requires additional backend changes and accepts the security risk.

- [ ] **Other:** ______

**Notes:**

---

## Risks — may cause rework if not resolved

---

### R1 — spec-01: No brute-force protection on the login endpoint

**Context:** The current `AuthController` exposes `POST /api/auth/login` with no rate limiting, account lockout, or CAPTCHA. An attacker can perform unlimited credential-stuffing or brute-force attempts. If the team plans to add protection later, it should be captured as an NFR now to avoid retrofitting security into an already-released endpoint.

**Options:**

- [ ] **A)** Add an NFR to the spec for rate limiting (e.g., max 5 failed attempts per minute per IP) — deferred to a future story but documented as a constraint.
- [x] **B)** Include rate limiting as a functional requirement in this story (must-have for acceptance).
- [ ] **C)** Explicitly out-of-scope for RF-02; no NFR documented here.

- [ ] **Other:** ______

**Notes:**

---

## Minor inconsistencies

---

### M1 — spec-01: Error response body format inconsistency

**Context:** On a failed login, `AuthController` currently returns a plain `String` body (e.g., `"Invalid username or password"`) with HTTP 401. Other API endpoints are expected to return structured JSON. Returning a raw string breaks client-side consistency and makes it harder for the frontend to parse and display the message uniformly.

**Options:**

- [x] **A)** Return a structured JSON error body `{ "message": "Invalid username or password" }` — consistent with REST conventions and easier for the frontend to consume.
- [ ] **B)** Keep the plain string response — simpler, acceptable if the frontend handles both formats.

- [ ] **Other:** ______

**Notes:**

---

## Summary (Only for Agent, do not edit this table manually)

| ID | Spec | Impact  | Short description                        | Decision |
|----|------|---------|------------------------------------------|----------|
| B1 | 01   | Blocker | Username enumeration security risk       | ___      |
| R1 | 01   | Risk    | No brute-force / rate-limit protection   | ___      |
| M1 | 01   | Minor   | Error response format (string vs. JSON)  | ___      |
