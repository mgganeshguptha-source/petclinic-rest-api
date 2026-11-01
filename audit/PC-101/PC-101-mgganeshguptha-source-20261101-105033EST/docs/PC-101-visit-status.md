# PC-101: Add a status to each visit in the Visit API

## Summary

As a clinic receptionist, I want each visit to carry a status (`SCHEDULED`, `COMPLETED` or `CANCELLED`), so that the front desk can see which visits still need to happen.

Requires a new or changed contract from: none.

## Expected behaviour

- A visit has a `status` with exactly one of three values: `SCHEDULED`, `COMPLETED`, `CANCELLED`.
- The values are defined once, as an enum schema (e.g. `VisitStatus`) in `src/main/resources/openapi.yml`, and referenced by the visit schemas. The API is contract-first: DTOs and API interfaces are generated from `openapi.yml` at build time, so the YAML changes first and the hand-written code must match the generated types.
- `status` is returned on every visit in every response that contains visits:
  - `GET /api/visits`
  - `GET /api/visits/{visitId}`
  - visits nested in pets and owners, in both API versions (e.g. `GET /api/owners/{ownerId}`, `GET /api/v2/owners`, `GET /api/v2/pets`).
- `status` is optional on create and update:
  - `POST /api/visits`, `POST /api/owners/{ownerId}/pets/{petId}/visits`, `PUT /api/visits/{visitId}`.
  - Absent on create: saved as `SCHEDULED`.
  - Absent on update: the stored status is kept.
  - Any other value (e.g. `DONE`, `completed`, empty string): HTTP 400, nothing saved or changed.
  - `"status": null` is treated as absent (no 400).
- Storage: new column `status` in the `visits` table, `NOT NULL`, default `SCHEDULED`, so existing rows become `SCHEDULED`.
  - H2, MySQL, PostgreSQL: `status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'`
  - HSQLDB: `status VARCHAR(20) DEFAULT 'SCHEDULED' NOT NULL` (DEFAULT must come before NOT NULL).
- Only an invalid `status` value returns 400. Every other malformed or unreadable request body keeps its current response on all endpoints.

## Acceptance criteria

| ID | Criterion |
|----|-----------|
| AC1 | `openapi.yml` defines an enum with exactly `SCHEDULED`, `COMPLETED`, `CANCELLED`; the visit response schema includes `status` referencing it. |
| AC2 | `GET /api/visits/{visitId}` for a seeded visit returns `"status": "SCHEDULED"`. |
| AC3 | `POST /api/visits` without `status` returns 201; the created visit has `"status": "SCHEDULED"`. |
| AC4 | `POST /api/visits` with `"status": "COMPLETED"` returns 201; a following GET returns `"status": "COMPLETED"`. |
| AC5 | `PUT /api/visits/{visitId}` with `"status": "CANCELLED"` returns 204; a following GET returns `"status": "CANCELLED"`. A PUT without `status` leaves the stored status unchanged. |
| AC6 | `POST` or `PUT` with `"status": "DONE"` returns 400; the visit is not created or changed. |
| AC7 | `GET /api/owners/{ownerId}`, `/api/v2/owners` and `/api/v2/pets` return `status` on every nested visit. |
| AC8 | Status is saved and read correctly with every persistence profile the existing tests cover (JDBC on H2 and HSQLDB, JPA, Spring Data JPA). Existing `ClinicService*Tests` and visit controller tests pass, plus new tests for the cases above. |
| AC9 | `./mvnw test` passes. |
| AC10 (CL-1) | `"status": null` on create or update is treated as absent: create saves `SCHEDULED`; update keeps the stored status. No 400. |
| AC11 (CL-2) | Only an invalid `status` value returns 400. Other malformed bodies keep their current response; no new 400 cases beyond `status`. |

## Scope

In scope:
- `src/main/resources/openapi.yml` (enum and visit schemas).
- `model/Visit.java` (new field stored as the enum name), plus a model enum if needed.
- `mapper/VisitMapper.java`, only if MapStruct cannot map the field by name.
- `repository/jdbc/` visit code: `JdbcVisitRepositoryImpl`, `JdbcVisitRowMapper`, `JdbcPetVisitExtractor`.
- `rest/controller/v1/VisitRestControllerV1.java`, `OwnerRestControllerV1.java` (add-visit endpoint), and v2 controllers only as far as needed for defaulting and keep-on-update rules.
- `db/h2`, `db/hsqldb`, `db/mysql`, `db/postgres` `schema.sql` (new column). `db/hsqldb/data.sql` (and any other visit insert without a column list): add the column list so rows load and take the default. No other seed-data changes.
- Tests under `src/test/java`.

Out of scope: Angular front end (PC-102), security/roles, changes to pets, owners, vets or specialties beyond the nested visit `status`, Docker and Kubernetes files, Postman tests.

## Implementation notes

- Keep the three persistence implementations (`jpa`, `springdatajpa`, `jdbc`) in step; a field added to one must be handled in all active ones.
- Use the Spring `@Transactional` annotation in any service code touched.
- Defaulting (create) and keep-on-update are applied in the service/controller layer, not in the client.
- Attachments: none.

## Clarification answers

None recorded in the story, so no service decisions were captured.
