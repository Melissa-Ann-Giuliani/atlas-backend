# User Story Description

This story covers the registration of a new auditor into the system by a Global Administrator. It is essential for onboarding auditors who will perform faculty evaluations.

---

### # Story (INVEST)

**As** a Global Administrator  
**I want** to register a new auditor in the system  
**So that** they can access the system and perform audits within the faculty.

---

### # Analysis

#### Approach

We will create a new vertical slice containing the registration endpoint and frontend form. The flow includes validating that the user does not exist (by username or email), generating a temporary password, saving the user as "Active" with the "Auditor" role and faculty position, and finally triggering an email with the temporary credentials.

---

### # Context

[raw-requirement.md](./raw-requirement.md)

---

### # Acceptance Criteria

**Scenario 1: Successful Auditor Registration (Happy Path)**
* **Given** the Global Administrator is logged in and navigates to the New User section
* **When** they fill in username, email, names, surnames, select the "Auditor" role, and choose a valid faculty position (Enum: Director, Vicedirector, Secretaría Académica)
* **And** they submit the registration form
* **Then** the system verifies the auditor is not previously registered (uniqueness checked on both username and email)
* **And** the system generates a secure temporary password (complexity same as Administrador Global and Administrador de Unidad)
* **And** the new auditor is saved with an "Active" status
* **And** the auditor appears in the faculty's user list
* **And** the admin only sees a success message (password is ONLY sent via email)
* **And** a confirmation email is sent to the provided email address containing the temporary password (failure handling same as Administrador Global and Administrador de Unidad).

**Scenario 2: Auditor Already Exists (Unhappy Path)**
* **Given** the Global Administrator is logged in and navigates to the New User section
* **When** they attempt to register an auditor with an email or username that already exists in the system
* **Then** the system rejects the registration
* **And** the system shows an error message indicating that the user is already registered.

**Scenario 3: Missing Required Fields (Unhappy Path)**
* **Given** the Global Administrator is logged in and navigates to the New User section
* **When** they submit the form without one of the required fields (username, email, names, surnames, role, or position)
* **Then** the system rejects the registration
* **And** prompts the administrator to complete all required fields.

---

### # Test Plan

#### Context

Requires SMTP server configuration (or mock) to test email delivery.

#### Scenarios

| Scenario | Type | Description |
|---|---|---|
| 1 | Success | Register auditor with all valid fields, verify DB record and email sent. |
| 2 | Error | Register auditor with existing email, verify rejection. |
| 3 | Error | Register auditor with existing username, verify rejection. |
| 4 | Error | Submit empty or partial form, verify validation errors. |

---

### # Non-functional notes

- **Security**: The generated password must be cryptographically secure and hashed before storing in the database. The plain text password is only used once in the email template.
- **Performance**: Email sending should ideally be asynchronous to not block the HTTP response of the registration.
