# Open Questions for RF-30

This document tracks unresolved business or technical questions regarding the teacher registration requirement.

## 1. What defines "Teacher already registered"?
Currently, the requirement states "El docente no debe estar previamente registrado en el sistema". Which fields specifically determine uniqueness?
- [ ] DNI only
- [ ] CUIL only
- [ ] Institutional Email only
- [ ] Combination of DNI and Email
- [x] Other: (Assumed DNI, CUIL, and Email must be unique across all users)

## 2. Mandatory Fields
The requirement lists: `nombre de usuario, apellidos y nombres, dni, cuil, género, fecha de nacimiento, domicilio, email institucional, teléfonos, fecha de ingreso a la facultad, antigüedad`. Are all of these fields mandatory?
- [ ] Yes, all are strictly mandatory.
- [x] No, some are optional. (Please specify which: telephones, gender, etc.?)

## 3. Email Generation
Should the temporary password have specific complexity requirements?
- [ ] Standard 8-character alphanumeric
- [x] Other: Same as all the other users (admin de unidad, admin global and auditor) 

## Summary (Only for Agent, do not edit this table manually)
| Question | Decision | Status |
|----------|----------|--------|
| Uniqueness | DNI, CUIL, and Email must be unique across all users | RESOLVED |
| Mandatory fields | gender, telephones, date of birth, and domicile are optional | RESOLVED |
| Password complexity | Same as other users (12-char random UUID string) | RESOLVED |
