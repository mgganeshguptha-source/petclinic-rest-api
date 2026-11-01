# Context: Add a status to each visit in the Visit API

Generated: 2026-11-01 15:51:29 UTC (CI mode, no questions asked)
Story: Add a status to each visit in the Visit API
Quality score: 6/10. The story-level ACs were not retained verbatim in this run, so the ACs below are reconstructed from the story intent and must be checked against the original story text.

## 1. Story summary

Each visit exposed by the Visit API carries a `status` value. The value is a fixed enum defined once in `openapi.yml`. The database stores it as a NOT NULL column with default `SCHEDULED`. Create, read, update, and list responses include the field.

## 2. Acceptance criteria (EARS)

- AC-1: The system shall define a `VisitStatus` enum once in `src/main/resources/openapi.yml`, with `SCHEDULED` as one of its values.
- AC-2: The system shall add a `status` property referencing `VisitStatus` to the `VisitFields` schema, so that `Visit` and `VisitDto` expose it.
- AC-3: When a visit is created without a `status`, the system shall persist it as `SCHEDULED`.
- AC-4: The `visits.status` column shall be NOT NULL with default `SCHEDULED` in every DB profile (h2, hsqldb, mysql, postgres).
- AC-5: When a `GET` is made on `/api/v1/visits` or `/api/v1/visits/{visitId}`, the response shall include `status` for each visit.
- AC-6: When an update is made to a visit with a valid `status`, the system shall persist and return the new value.
- AC-7: When a request body contains a `status` value outside the enum, the system shall return 400 with a ProblemDetail body and shall not persist anything.
- AC-8: The `/owners/{ownerId}/pets/{petId}/visits` nested representation shall include `status` wherever a visit is embedded.
- AC-9: The system shall keep existing `@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")` guards on all visit endpoints. No new endpoint is exposed without one.
- AC-10: When `status` is omitted or null on update, the system shall leave the stored value unchanged. Null is treated as absent. (Resolves CL-1 from the story.)
- AC-11: The system shall return 400 only for an invalid enum value. A missing or null `status` shall never produce 400. (Resolves CL-2 from the story.)

## 3. Scope

In scope:
- `openapi.yml`: `VisitStatus` enum, `status` on `VisitFields`, and nested visit schemas.
- `model/Visit.java`: new `status` field with an enum type.
- `mapper/VisitMapper.java`: MapStruct maps `status` by name, so no explicit mapping is required unless names diverge.
- `rest/controller/v1/VisitRestControllerV1.java`: no logic change expected beyond passing the DTO through.
- `db/{h2,hsqldb,mysql,postgres}/schema.sql` and `data.sql`: add the column and default. Seed rows get an explicit status.
- `repository/jdbc/` visit row mapper and extractor, if the JDBC path is active.

Out of scope:
- New visit endpoints or new HTTP methods.
- Status transition rules (for example, SCHEDULED to COMPLETED ordering). Not in the story.
- Changes to v2 controllers, unless the v2 schema shares `VisitFields`.
- Generated sources under `target/generated-sources/openapi` (never hand-edit).

## 4. Current state (verified by read)

- `openapi.yml` `VisitFields` (around line 2264): `date` and `description` only. `Visit` (around line 2284) is an `allOf` of `VisitFields` plus `id` and `petId`. No `status` exists.
- `model/Visit.java`: `@Entity @Table(name="visits")`, fields `date` (`visit_date`, DATE), `description` (`@NotEmpty`), and `pet` (`@ManyToOne`). No `status` field. Extends `BaseEntity`.
- `mapper/VisitMapper.java`: maps `petId` to `pet.id` and ignores `id` and `pet` on the reverse path.
- `VisitRestControllerV1`: `@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")` on list, get, add, update, and delete. Uses `jakarta.transaction.Transactional` on delete. Other methods are unannotated.
- DB schema:
  - h2 `visits`: `visit_date DATE NOT NULL`, `description VARCHAR(255) NOT NULL`.
  - hsqldb `visits`: `description VARCHAR(255) NOT NULL`, no status column.
  - mysql `visits`: `description VARCHAR(255) NOT NULL`, no status column.
  - postgres `visits`: `description TEXT NOT NULL`, no status column.
- Seed data: 4 visits in each profile's `data.sql`. Hsqldb and mysql use positional `VALUES`. Postgres uses `INSERT ... SELECT ... WHERE NOT EXISTS`. H2 uses a named column list.

## 5. Constraints and rules

- Contract-first: change `openapi.yml` first. DTOs and API interfaces are generated at build time.
- Validation: Jakarta Validation on DTOs. Invalid enum values are rejected by the generated enum deserialiser, which produces 400 through `ExceptionControllerAdvice`.
- Error responses: RFC 9457 ProblemDetail. No stack traces or internal messages.
- Controllers return DTOs only, never entities.
- Constructor injection only. Use the Spring `@Transactional` in new service code.
- Each repository method must be added to every active implementation (jpa, springdatajpa, jdbc).
- HSQLDB syntax: `DEFAULT` must come before `NOT NULL` in column definitions.
- Logging: never log visit payloads or owner, pet, or visit data.
- HIPAA: visit records are clinical data. Any new read or write path needs an audit TODO (`// TODO HIPAA: emit audit event (actor, action, resource, timestamp)`) if no audit emission exists nearby.

## 6. Design

Design trigger: YES. The enum becomes a new value in the published API contract and spans the entity, the schema in four DB profiles, and the JDBC and JPA repository paths. Changing the contract affects clients.

Trade-offs:
- Enum in `openapi.yml`: the single source of truth. Generated Java enum. Chosen per the story.
- Column default `SCHEDULED`: keeps existing rows valid without a backfill. Seed data should still set status explicitly for clarity.

## 7. Feasibility

Verdict: GO. The entity, mapper, controller, and four schema files all exist and are structurally simple. The only risks are the JDBC row mapper and the HSQLDB default syntax.

## 8. Risks

- Generated enum name clashes with an existing type. Low risk. Check the generated `rest.dto` package.
- Hsqldb `data.sql` positional inserts break when a column is added. Must be updated in the same change.
- Postgres `NOT EXISTS` seeds do not re-apply the default for existing rows. Use an explicit `status` value in the seed.
- Missed JDBC path leaves `status` null on read. Check `repository/jdbc/` before finishing.

## 9. Open items

- The enum values beyond `SCHEDULED` were not confirmed in this run. Only `SCHEDULED` is stated in the story. Confirm the full list before generating the enum.
- Confirm whether the v2 controllers share `VisitFields`. If they do, the change also reaches v2.

## 10. Tests to add

- Unit: `VisitMapper` maps `status` both ways.
- Controller (`@WebMvcTest`): valid `status` round-trips, invalid enum returns 400 ProblemDetail, null `status` on update leaves the value unchanged.
- Repository: default `SCHEDULED` applied on insert when `status` is omitted.
- Security: each visit endpoint rejects a caller without `ROLE_OWNER_ADMIN`.

## 11. Files to change

1. `src/main/resources/openapi.yml`
2. `src/main/java/org/springframework/samples/petclinic/model/Visit.java`
3. `src/main/java/org/springframework/samples/petclinic/mapper/VisitMapper.java` (only if names diverge)
4. `src/main/resources/db/h2/schema.sql` and `data.sql`
5. `src/main/resources/db/hsqldb/schema.sql` and `data.sql`
6. `src/main/resources/db/mysql/schema.sql` and `data.sql`
7. `src/main/resources/db/postgres/schema.sql` and `data.sql`
8. `repository/jdbc/` visit row mapper, if present on the active path
9. Matching test classes under `src/test/java`

## 12. Verification

- `mvn -q -DskipTests=false test` for the affected test classes.
- Start with the h2 profile and call `GET /api/v1/visits` with basic auth. Expect `status` on each item.
- Repeat with hsqldb to confirm the HSQLDB `DEFAULT` syntax.
- Confirm JaCoCo still meets the 90% line and 80% branch thresholds for changed classes.
