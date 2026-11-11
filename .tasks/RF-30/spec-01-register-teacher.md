# User Story Description

The system must allow Global or Unit Administrators to register new teachers, capturing their personal, academic, and contact details. Additionally, the system should save the provided seniority (defaulting to 0) and send a confirmation email with a temporary password to the registered teacher.

---

### # Story (INVEST)

**As a** Global Administrator or Unit Administrator  
**I want** to register a new teacher in the system by entering their personal and institutional details  
**So that** the teacher can access the platform and be officially listed in the faculty's registry.

---

### # Analysis

#### Approach

- The registration will be exposed via a REST endpoint accepting teacher details.
- Validations will ensure the teacher isn't already registered (DNI, CUIL, and institutional email must be individually unique across all users).
- **Legajo (N° de legajo):** optional integer field (`legajo` in `DocenteRequest`, persisted in `docentes.docente_legajo`). When provided, it must be unique among teachers. It is NOT derived from `usuario_id` or DNI; if it is not provided, it stays `null` and consumers must show it as "not specified".
- The fields gender, telephones, domicile, date of birth, and legajo are optional; all other fields are mandatory.
- The seniority (antiguedad previa) is directly provided by the user. If left blank, it defaults to 0.
- An asynchronous event or service should handle sending the confirmation email with the generated temporary password (a 12-character random UUID string, consistent with other roles).

---

### # Acceptance Criteria

**Scenario 1: Successful teacher registration (Happy Path)**
* **Given** the teacher's DNI, CUIL, and institutional email do not exist in the system
* **When** the administrator submits the valid registration form for a new teacher
* **Then** the teacher is saved in the database under the 'Docente' role
* **And** the teacher is listed in the faculty's registry
* **And** an email with a temporary password is sent to the provided institutional email.

**Scenario 2: Teacher with prior seniority**
* **Given** the administrator is registering a teacher
* **When** the administrator inputs prior seniority (e.g., 5 years)
* **Then** the teacher's profile is created with 5 years of seniority.

**Scenario 3: Teacher without prior seniority**
* **Given** the teacher does not have prior seniority
* **When** the administrator submits the registration leaving seniority blank
* **Then** the teacher's profile is created with 0 years of seniority.

**Scenario 4: Teacher already exists (Unhappy flow)**
* **Given** a user is already registered with the same DNI, CUIL, or email
* **When** the administrator attempts to register the teacher again
* **Then** the system rejects the registration
* **And** returns a validation error indicating the user already exists.

**Scenario 5: Registration with missing mandatory fields (Unhappy flow)**
* **Given** the administrator leaves mandatory fields blank (but provides optional ones like gender or telephones)
* **When** they submit the registration
* **Then** the system rejects the registration
* **And** returns a validation error detailing the missing mandatory fields.

**Scenario 6: Teacher registration with legajo**
* **Given** no other teacher has legajo `12345`
* **When** the administrator submits a valid registration including legajo `12345`
* **Then** the teacher is saved with legajo `12345`
* **And** the legajo is returned in `GET /api/docentes` (`DocenteListDTO.legajo`) and `GET /api/docentes/{id}` (`DocenteDetailsDTO.legajo` / `datosPersonales.legajo`).

**Scenario 7: Duplicate legajo (Unhappy flow)**
* **Given** a teacher is already registered with legajo `12345`
* **When** the administrator attempts to register another teacher with legajo `12345`
* **Then** the system rejects the registration with the error "Legajo ya existe".

**Scenario 8: Teacher registration without legajo**
* **Given** the administrator leaves legajo blank
* **When** they submit an otherwise valid registration
* **Then** the teacher is saved with `legajo = null` (no fallback to user ID or DNI).

---

### # Test Plan

#### Context

- SMTP or email mock server should be configured to verify the confirmation email generation.
- The administrator must be authenticated and authorized.

#### Scenarios

| # | Type | Description |
|---|---|---|
| 1 | Success | Register a valid teacher with 0 seniority, verify DB record and email sent. |
| 2 | Success | Register a valid teacher with 5 years seniority, verify DB record. |
| 3 | Error | Attempt to register with an existing DNI, verify 400 Bad Request/Conflict error. |
| 4 | Error | Attempt to register with missing mandatory fields, verify validation error. |
| 5 | Success | Register a valid teacher with legajo, verify it is persisted and exposed in list/detail DTOs. |
| 6 | Error | Attempt to register with an existing legajo, verify "Legajo ya existe" error. |
| 7 | Success | Register a valid teacher without legajo, verify `legajo` is `null`. |

---

### # Non-functional notes

- **Security:** Passwords must be generated as a 12-character random string from a UUID (same as other user roles) and hashed using BCrypt before storing.
- **Performance:** Email sending should be handled asynchronously so it does not block the API response.
