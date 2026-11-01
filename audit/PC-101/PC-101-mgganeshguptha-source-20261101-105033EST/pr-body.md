# Add a status to each visit in the Visit API

## Summary

Each visit now carries a `status` with exactly one of three values: `SCHEDULED`, `COMPLETED`, or `CANCELLED`. This lets the front desk see which visits still need to happen.

Story: As a clinic receptionist, I want each visit to carry a status (scheduled, completed or cancelled), so that the front desk can see which visits still need to happen.

## Changes

- **Contract (`src/main/resources/openapi.yml`):** new `VisitStatus` enum schema with `SCHEDULED`, `COMPLETED`, `CANCELLED`. Visit response schemas reference it. Create and update request schemas accept an optional `status`. The YAML is changed first; DTOs and API interfaces are regenerated at build time.
- **Model (`model/Visit.java`):** new status field, persisted as the enum name. A model enum is added if MapStruct cannot map the field by name.
- **Mapper (`mapper/VisitMapper.java`):** changed only if MapStruct cannot map `status` by name.
- **JDBC (`repository/jdbc/`):** `JdbcVisitRepositoryImpl`, `JdbcVisitRowMapper`, and `JdbcPetVisitExtractor` read and write the `status` column.
- **Controllers:** `VisitRestControllerV1`, `OwnerRestControllerV1` (add-visit endpoint), and v2 controllers, only as far as needed for defaulting and keep-on-update rules.
- **Schema (`db/h2`, `db/hsqldb`, `db/mysql`, `db/postgres` `schema.sql`):** new `status` column, NOT NULL, default `SCHEDULED`. HSQLDB uses `DEFAULT` before `NOT NULL`.
- **Seed data:** `db/hsqldb/data.sql` gets an explicit column list for visit inserts so rows load and take the default. No other seed-data changes.
- **Tests (`src/test/java`):** new cases for the behaviour below, across the existing `ClinicService*Tests` and visit controller tests.

## Behaviour

- `status` is returned on every visit in every response that contains visits: `GET /api/visits`, `GET /api/visits/{visitId}`, and nested visits under pets and owners in both API versions.
- **Create** (`POST /api/visits`, `POST /api/owners/{ownerId}/pets/{petId}/visits`): absent or `null` status saves as `SCHEDULED`.
- **Update** (`PUT /api/visits/{visitId}`): absent or `null` status keeps the stored status.
- An invalid value (e.g. `DONE`, `completed`, empty string) returns **400** and nothing is saved or changed.
- No other malformed-body responses change. This story adds no 400 cases beyond invalid `status`.

## Acceptance criteria

- [ ] AC1: `openapi.yml` defines the enum with exactly the three values; visit schema references it.
- [ ] AC2: `GET /api/visits/{visitId}` for a seeded visit returns `"status": "SCHEDULED"`.
- [ ] AC3: `POST /api/visits` without `status` returns 201 with `SCHEDULED`.
- [ ] AC4: `POST /api/visits` with `COMPLETED` returns 201; following GET returns `COMPLETED`.
- [ ] AC5: `PUT` with `CANCELLED` returns 204; following GET returns `CANCELLED`. `PUT` without `status` leaves it unchanged.
- [ ] AC6: `POST` or `PUT` with `DONE` returns 400; visit not created or changed.
- [ ] AC7: `GET /api/owners/{ownerId}`, `/api/v2/owners`, and `/api/v2/pets` return `status` on every nested visit.
- [ ] AC8: Status saved and read correctly under JDBC (H2, HSQLDB), JPA, and Spring Data JPA.
- [ ] AC9: `./mvnw test` passes.
- [ ] AC10: `"status": null` is treated as absent on create and update. No 400.
- [ ] AC11: Only an invalid `status` value returns 400; other malformed bodies keep current responses.

## Out of scope

- Angular front end (PC-102)
- Security and roles
- Pets, owners, vets, or specialties beyond the nested visit `status`
- Docker and Kubernetes files
- Postman tests

## Testing

- `./mvnw test` (AC9)
- Visit controller and `ClinicService*Tests` across all persistence profiles

## Notes for reviewers

- The DB default `SCHEDULED` backfills existing rows.
- HSQLDB column definition order differs (`DEFAULT` before `NOT NULL`); check it against the other schemas.
- `DataIntegrityViolationException` mapping is unchanged by this PR.
