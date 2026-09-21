# User Story Description

The system must allow a Global Administrator to register a new Unit Administrator (Administrador de Unidad) so they can manage their respective faculty department, institute, or center. This process automatically generates a temporary password and sends it via email.

---

### # Story (INVEST)

**As** a Global Administrator  
**I want** to register a new Unit Administrator  
**So that** they can manage their corresponding dependency (department, institute, or center) within the faculty.

---

### # Analysis

#### Approach

- The system needs an endpoint to create the user.
- A random password will be generated upon creation.
- The user will be assigned the "Unit Administrator" role and "Activo" status automatically.
- Upon successful creation, an email will be dispatched with the temporary password.
- Out of scope: The password change process inside the profile (will be covered in a different story).

---

### # Acceptance Criteria

**Scenario 1: Successful Registration of Unit Administrator (Happy Path)**
* **Given** the Global Administrator is logged into the system and on the "New User" screen
* **When** they fill in "nombre de usuario", "correo electrónico", "apellido/s" and "nombre/s" with valid and unique data
* **And** they select "Administrador de Unidad" as Role
* **And** they select a valid position (Director, Vicedirector, Secretaría Administrativa) and dependency (department, institute, center)
* **And** they submit the form
* **Then** the user is created with the "Activo" status
* **And** a temporary password is automatically generated
* **And** the user appears in the faculty's user list
* **And** a confirmation email with the temporary password is sent to the provided email address.

**Scenario 2: Registration fails due to existing user**
* **Given** the Global Administrator is on the "New User" screen
* **When** they attempt to register a Unit Administrator with an email or username that is already registered in the system
* **Then** the system shows an error message indicating the user already exists
* **And** the registration is aborted.

---

### # Non-functional notes

- The generated password must be a 12-character strong password (letters, numbers, symbols).
- The password must be hashed before saving to the database. It should never be logged or visible in plain text.
- Emails should be sent asynchronously using a background job/queue (e.g., BullMQ, RabbitMQ) to not block the HTTP response of the registration.
