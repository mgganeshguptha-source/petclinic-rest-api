# Plan for: Add a status to each visit in the Visit API

**Source:** Story text supplied in the harness prompt (AC1–AC11, CL-1, CL-2) + `.github/story-context-files/visit-status-context-261101-155129.md` + `.github/copilot-instructions.md`
**Stack:** Backend only (Spring Boot 4.1.1, Spring MVC, JPA + JDBC + Spring Data JPA, MapStruct, contract-first OpenAPI generation)
**Total steps:** 16
**Unresolved clarifications:** None. CL-1 (null = absent) and CL-2 (only invalid enum values return 400) are resolved in the story as AC10 and AC11.

---

## Acceptance criteria coverage

| AC | Criterion (abbreviated) | Covered by |
|---|---|---|
| AC1 | `openapi.yml` enum `SCHEDULED/COMPLETED/CANCELLED`; visit schema has `status` | Step 3, Step 14 |
| AC2 | GET seeded visit returns `SCHEDULED` | Step 6, Step 7, Step 12, Step 15 |
| AC3 | POST without `status` → 201, `SCHEDULED` | Step 4, Step 8, Step 9, Step 13 |
| AC4 | POST with `COMPLETED` → 201, later GET shows `COMPLETED` | Step 7, Step 9, Step 13 |
| AC5 | PUT with `CANCELLED` → 204, GET shows it; PUT without `status` keeps stored value | Step 7, Step 9, Step 13 |
| AC6 | POST/PUT with `DONE` → 400, nothing saved | Step 2, Step 10, Step 13 |
| AC7 | `status` on nested visits in `/api/owners/{id}`, `/api/v2/owners`, `/api/v2/pets` | Step 11, Step 13 |
| AC8 | Persisted and read correctly on JDBC H2, JDBC HSQLDB, JPA, Spring Data JPA | Step 5, Step 7, Step 12 |
| AC9 | `./mvnw test` passes | Step 14 |
| AC10 | `"status": null` treated as absent (create → SCHEDULED, update → unchanged), no 400 | Step 8, Step 9, Step 13 |
| AC11 | Only invalid values give 400; other malformed bodies unchanged | Step 2, Step 10, Step 13 |

Every AC is covered by at least one step. No AC is marked `[WITHDRAWN]`. No AC is marked `[ASSUMED]`.

---

## Before you execute any step

1. Keep the story text (AC1–AC11) in the chat context. Re-paste it after any session restart.
2. Run the steps in order. Each step's `Depends on:` lists its direct prerequisites.
3. Do not hand-edit anything under `target/generated-sources/openapi`. Change `openapi.yml` and rebuild.
4. Generated class names must be taken from the build output, not guessed. Step 1 and Step 3 confirm them.

---

## Pre-flight

The plan assumes:

1. **Stack:** Spring Boot REST API with JPA, JDBC, and Spring Data JPA repository implementations, selected by profile. Model classes and DTOs are generated from `src/main/resources/openapi.yml` at build time.
2. **Behaviour preservation:**
   - `GET /api/visits` still returns 404 when the list is empty (existing behaviour; not changed by this story).
   - `POST /api/visits` still returns 201 with a `Location` header.
   - `PUT /api/visits/{visitId}` still returns 204.
   - `@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")` stays on every visit endpoint. No endpoint is added.
   - Nested visit shape is unchanged apart from the added `status`.
   - `date` and `description` update behaviour is unchanged.
3. **Non-functional handling:** The story sets no performance or load targets. Nothing is verified beyond the test suite.
4. **Assumed criteria:** No assumed criteria. CL-1 and CL-2 are resolved by the story (AC10, AC11).
5. **Context file mismatch:** The context file's AC-1…AC-11 were reconstructed in its own run (quality 6/10) and do not match the story's numbering. This plan uses the story's AC1–AC11. Its claim that seed rows get an explicit status is overridden: the story says `data.sql` should take the column default wherever a column list is present.
6. **Enum value spelling:** `SCHEDULED`, `COMPLETED`, and `CANCELLED` are exact upper-case strings. Lower-case and other values are invalid.
7. **Generated enum name:** The OpenAPI enum schema `VisitStatus` is expected to generate `org.springframework.samples.petclinic.rest.dto.VisitStatusDto`. Step 1 confirms this from the build output. Until then, all references use the fully qualified name.
8. **Model enum name:** The entity enum is `org.springframework.samples.petclinic.model.VisitStatus`. It has the same simple name as the generated DTO enum, so every file that imports both must use fully qualified names for one of them.

If any of these assumptions are wrong, stop and revise before executing Step 3.

---

## Impacted Files

| ID | Path | Role |
|----|------|------|
| F1 | `src/main/resources/openapi.yml` | Add `VisitStatus` enum schema; add `status` to `VisitFields` (inherited by `Visit`) |
| F2 | `src/main/java/org/springframework/samples/petclinic/model/VisitStatus.java` | **New.** Entity-side enum `SCHEDULED, COMPLETED, CANCELLED` |
| F3 | `src/main/java/org/springframework/samples/petclinic/model/Visit.java` | New `status` field: `@Enumerated(STRING)`, `NOT NULL`, initialised to `SCHEDULED` |
| F4 | `src/main/java/org/springframework/samples/petclinic/mapper/VisitMapper.java` | Default null `status` to `SCHEDULED` on create; add enum conversion method for update |
| F5 | `src/main/java/org/springframework/samples/petclinic/repository/jdbc/JdbcVisitRepositoryImpl.java` | Write `status` on insert and update; select `status` in every query |
| F6 | `src/main/java/org/springframework/samples/petclinic/repository/jdbc/JdbcVisitRowMapper.java` | Read `status` column into the entity |
| F7 | `src/main/java/org/springframework/samples/petclinic/repository/jdbc/JdbcPetVisitExtractor.java` | Child visit mapping through `JdbcVisitRowMapper`; verify the SELECT that feeds it |
| F8 | `src/main/java/org/springframework/samples/petclinic/repository/jdbc/` (pet JDBC repository, file name confirmed in Step 1) | Pet-with-visits SELECT must include the `status` column |
| F9 | `src/main/java/org/springframework/samples/petclinic/rest/controller/v1/VisitRestControllerV1.java` | `updateVisit`: keep current status when the request status is null |
| F10 | `src/main/java/org/springframework/samples/petclinic/rest/controller/v1/OwnerRestControllerV1.java` | `addVisitToOwner`: uses `VisitMapper.toVisit(VisitFieldsDto)`; verify, probably no edit |
| F11 | `src/main/java/org/springframework/samples/petclinic/rest/advice/ExceptionControllerAdvice.java` | Conditional. Targeted 400 for an invalid `status` value only, if not already covered (see Step 2) |
| F12 | `src/main/java/org/springframework/samples/petclinic/rest/controller/v2/OwnerRestControllerV2.java` | Verify nested `status` in responses; probably no edit |
| F13 | `src/main/java/org/springframework/samples/petclinic/rest/controller/v2/PetRestControllerV2.java` | Verify nested `status` in responses; probably no edit |
| F14 | `src/main/resources/db/h2/schema.sql` | Add `status VARCHAR(20) DEFAULT 'SCHEDULED' NOT NULL` |
| F15 | `src/main/resources/db/hsqldb/schema.sql` | Add `status VARCHAR(20) DEFAULT 'SCHEDULED' NOT NULL` (DEFAULT before NOT NULL) |
| F16 | `src/main/resources/db/mysql/schema.sql` | Add `status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'` |
| F17 | `src/main/resources/db/postgres/schema.sql` | Add `status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'` |
| F18 | `src/main/resources/db/hsqldb/data.sql` | Positional `INSERT INTO visits VALUES (...)` → add column list so rows take the default |
| F19 | `src/main/resources/db/mysql/data.sql` | Only if the visit insert has no column list: add one. No status value |
| F20 | `src/main/resources/db/postgres/data.sql` | Only if the visit insert has no column list: add one. No status value |
| F21 | `src/main/resources/db/h2/data.sql` | Named column list already present. Verify only; probably no edit |
| F22 | `src/test/java/org/springframework/samples/petclinic/mapper/` (VisitMapper test, new or existing) | Mapper unit tests |
| F23 | `src/test/java/org/springframework/samples/petclinic/service/ClinicService*Tests` (JDBC H2/HSQLDB, JPA, Spring Data JPA) | Persistence round-trip tests |
| F24 | `src/test/java/org/springframework/samples/petclinic/rest/controller/v1/` visit and owner controller tests | Controller tests (201/204/400, nested status) |
| F25 | `src/test/java/org/springframework/samples/petclinic/rest/controller/v2/` owner and pet tests | Nested `status` on v2 responses |

> Later steps refer to files by ID. Step 1 may add IDs for files the seed missed. It must not renumber existing IDs.

---

## Signature changes

| File ID | Method | Before | After | Why the story needs it |
|---------|--------|--------|-------|------------------------|
| F4 | `VisitMapper.toVisit(VisitFieldsDto)` | returns `Visit` | unchanged signature; null `status` mapped to `SCHEDULED` | AC3, AC10 create default |
| F4 | `VisitMapper.toVisit(VisitDto)` | returns `Visit` | unchanged signature; null `status` mapped to `SCHEDULED` | AC3, AC10 create default |
| F4 | `VisitMapper` (new method) | — | `VisitStatus toVisitStatus(VisitStatusDto dto)` (addition) | Converts the request enum to the entity enum for the update path |
| F3 | `Visit` | — | new `getStatus()` / `setStatus(VisitStatus)` (addition) | AC1–AC5 |
| F5, F6, F7, F8 | JDBC visit and pet methods | unchanged | unchanged signatures; SQL and row mapping change | AC8 |

No existing public or interface method changes its signature or return type. The new `status` on the generated visit DTOs is a generated addition, not a hand-written change.

---

## Contract usage

None — this change reads no new or changed contract from another repo.

---

## Step 1 — Confirm the file set and the generated names

**Goal:** Confirm every file in the Impacted Files block, find every SQL SELECT or JPQL query that reads visits, confirm the v2 response mapping path, confirm how `ExceptionControllerAdvice` handles unreadable bodies, confirm the existing test class names, and confirm the generated enum class name. Add IDs for anything the seed list missed.
**Implements:** — (enabling step, no AC)
**Depends on:** —

**Suggested prompt:**

> Using the story text (AC1–AC11) in this chat, confirm the Impacted Files block in `.harness/prompt-steps.md`. Do not edit anything.
> 1. Search `src/main/java/**/repository/jdbc/` for `visit_date` and `visits` and list every SELECT that returns visit columns. Name each file and query.
> 2. Search `src/main/java/**/repository/springdatajpa/` and `repository/jpa/` for custom JPQL or `@Query` that selects visits. List any that would need `status`.
> 3. Open `ExceptionControllerAdvice` and state how `HttpMessageNotReadableException` and `InvalidFormatException` are currently mapped. Report the status code returned for an unknown enum string and for malformed JSON.
> 4. Confirm the v2 owner and pet controllers map visits through `VisitMapper`, MapStruct or another mapper, and list the mapper file names.
> 5. List the existing test classes that match `ClinicService*Tests`, the visit controller tests, and the owner and pet v1/v2 controller tests, with their paths.
> 6. Check whether the visit insert in `db/mysql/data.sql` and `db/postgres/data.sql` has a column list.
> Report the results as a list. Add a new ID for any file the seed list missed. Do not renumber existing IDs.

**Review checkpoint:** The report names the generated enum class (expected `VisitStatusDto`) and every SQL query that reads visits. The advice behaviour for unknown enum strings is recorded. If the advice maps unreadable bodies to 500, F11 becomes mandatory. Otherwise F11 stays conditional.

---

## Step 2 — Decide how an invalid `status` becomes 400 without changing other bodies

**Goal:** Choose how invalid enum values produce 400 with a `ProblemDetail` body, while every other malformed body keeps its current response (AC11).
**Implements:** AC6, AC11
**Depends on:** Step 1

**Suggested prompt:**

> Using the Step 1 report, propose 2 options for turning an invalid `status` value (for example `DONE`, `completed`, `""`) into HTTP 400 with the existing ProblemDetail shape:
> 1. Rely on the existing generic `HttpMessageNotReadableException` mapping in F11, if it already returns 400.
> 2. Add a targeted `@ExceptionHandler` in F11 for `InvalidFormatException` whose target type is the generated `VisitStatusDto` enum, returning 400. All other unreadable-body cases stay on their current handler.
> For each option, state the effect on the malformed-JSON response, on empty-string `""` handling, and on the response body. Do not write code. Recommend one.

**Review checkpoint:** The chosen option leaves malformed JSON and other unreadable bodies with exactly their current status and body (AC11). An empty string `""` is also 400, not silently treated as null. Confirm this before Step 10.

---

## Step 3 — Add the enum and the `status` property to the OpenAPI contract

**Goal:** Define `VisitStatus` once in `openapi.yml` and reference it from `VisitFields`, which `Visit` inherits.
**Implements:** AC1
**Depends on:** Step 1

**Suggested prompt:**

> Edit F1 (`src/main/resources/openapi.yml`) only.
> 1. Under `components/schemas`, add a `VisitStatus` schema: `type: string`, `enum: [SCHEDULED, COMPLETED, CANCELLED]`, with a title and description. Do not add a `default` keyword.
> 2. In `VisitFields.properties`, add `status` referencing `#/components/schemas/VisitStatus`. Do not add `status` to `required`. Keep `date` and `description` unchanged.
> 3. Do not change `Visit` directly. It inherits `status` through `allOf`.
> 4. Run `./mvnw -q -DskipTests generate-sources` and report the generated class name for the enum under `target/generated-sources/openapi` (expected `VisitStatusDto`), and confirm `VisitFieldsDto` and `VisitDto` contain `getStatus()`.
> Do not hand-edit generated sources.

**Review checkpoint:** The YAML has exactly three enum values, with no `default`. `status` is optional on `VisitFields`. The generated enum name is recorded for all later steps. If it is not `VisitStatusDto`, update every FQN reference before Step 4.

---

## Step 4 — Add the entity field and its default

**Goal:** Give `Visit` a `status` field stored as the enum name, NOT NULL, defaulting to `SCHEDULED`.
**Implements:** AC3 (entity-level default), AC8 (storage type)
**Depends on:** Step 1

**Suggested prompt:**

> Create F2 (`model/VisitStatus.java`) as a plain Java enum with constants `SCHEDULED, COMPLETED, CANCELLED`. Add no Javadoc beyond a one-line class comment.
> Then edit F3 (`model/Visit.java`):
> - Add `@Enumerated(EnumType.STRING) @Column(name = "status", nullable = false) private VisitStatus status = VisitStatus.SCHEDULED;`
> - Add `getStatus()` and `setStatus(VisitStatus)` in the same style as the existing accessors.
> Do not change the constructor, `date`, `description`, or `pet`.
>
> The entity enum is `org.springframework.samples.petclinic.model.VisitStatus`. Never import the generated `VisitStatusDto` into this file.

**Review checkpoint:** `Visit` has a non-null default. `EnumType.STRING` is used, so the column stores `SCHEDULED`, not an ordinal. The class compiles on its own.

---

## Step 5 — Add the `status` column to the four schema files

**Goal:** Add the NOT NULL column with default `SCHEDULED` in each profile's own syntax.
**Implements:** AC8 (persistence layer)
**Depends on:** Step 1

**Suggested prompt:**

> Edit only the `visits` CREATE TABLE in each file:
> - F14 (`db/h2/schema.sql`): `status VARCHAR(20) DEFAULT 'SCHEDULED' NOT NULL`
> - F15 (`db/hsqldb/schema.sql`): `status VARCHAR(20) DEFAULT 'SCHEDULED' NOT NULL` (HSQLDB requires DEFAULT before NOT NULL)
> - F16 (`db/mysql/schema.sql`): `status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'`
> - F17 (`db/postgres/schema.sql`): `status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'`
> Do not change any other table. Do not change the `CREATE INDEX` or `ALTER TABLE` statements.
> Check that `status` is not a reserved word in each database. Report any that is.

**Review checkpoint:** All four files have the column. HSQLDB uses DEFAULT before NOT NULL. No other table is touched. Note: `CREATE TABLE IF NOT EXISTS` (H2, MySQL, Postgres) will not add the column to an already-existing persistent table. This is a risk, recorded in the Risks section below.

---

## Step 6 — Update seed data so visit inserts take the default

**Goal:** Make seeded visits load with `SCHEDULED` through the default, with no explicit status value (AC2).
**Implements:** AC2
**Depends on:** Step 5

**Suggested prompt:**

> For each visit insert in F18 (`db/hsqldb/data.sql`), which uses positional VALUES, change it to a named column list: `INSERT INTO visits (id, pet_id, visit_date, description) VALUES (...)`. Keep the same values. Do not add a `status` value.
> Check F19 (`db/mysql/data.sql`) and F20 (`db/postgres/data.sql`). Only if their visit inserts have no column list, add one in the same way. Do not add a `status` value. If they already have a column list, do not edit them.
> Verify F21 (`db/h2/data.sql`) already has a named column list and needs no change.
> Do not change any other seed row or table.

**Review checkpoint:** Every seeded visit row loads, and no profile has a positional visit insert that would break on the new column. Seeded visits resolve to `SCHEDULED` after startup (this is checked again in Step 12).

---

## Step 7 — Make the JDBC repository read and write `status`

**Goal:** Write `status` on insert and update, and select it on every JDBC visit read (AC2, AC4, AC5, AC8).
**Implements:** AC2, AC4, AC5, AC8
**Depends on:** Step 4, Step 5

**Suggested prompt:**

> Edit F5 (`repository/jdbc/JdbcVisitRepositoryImpl.java`):
> 1. In `createVisitParameterSource`, add `.addValue("status", visit.getStatus().name())`.
> 2. In `save`, change the UPDATE to `UPDATE visits SET visit_date=:visit_date, description=:description, pet_id=:pet_id, status=:status WHERE id=:id`.
> 3. In `findByPetId`, `findById`, and `findAll`, add `status` to each SELECT column list.
> 4. In `JdbcVisitRowMapperExt.mapRow`, read `status` and set it on the visit.
> Edit F6 (`JdbcVisitRowMapper.java`): read `rs.getString("status")` and set `VisitStatus.valueOf(...)` on the visit. Use the entity enum.
> Edit F8 (the pet JDBC repository, named in Step 1): add the `visits.status` column to the pet-with-visits SELECT, so F7's extractor receives it.
> Do not change method signatures. Do not change the pet or owner queries beyond the visit columns.

**Review checkpoint:** Every SELECT that returns visit columns includes `status`. Every insert and update writes it. `status` is never null after a read. This step is checked against all four persistence profiles in Step 12.

---

## Step 8 — Map `status` in `VisitMapper`: default on create, enum conversion for update

**Goal:** On create, a null request `status` becomes `SCHEDULED`. The update path can convert the request enum to the entity enum (AC3, AC10).
**Implements:** AC3, AC10
**Depends on:** Step 3, Step 4

**Suggested prompt:**

> Edit F4 (`mapper/VisitMapper.java`).
> 1. On both `toVisit(VisitDto)` and `toVisit(VisitFieldsDto)`, add a mapping so a null request `status` becomes `org.springframework.samples.petclinic.model.VisitStatus.SCHEDULED`. Use `@Mapping(target = "status", source = "status", defaultExpression = "java(...)")` with the entity enum's fully qualified name. Do not use a bare `defaultValue`, and do not rely on the entity field initialiser, because MapStruct sets null explicitly.
> 2. Add `VisitStatus toVisitStatus(VisitStatusDto dto);` (fully qualified `VisitStatusDto` if Step 1 confirmed the name). MapStruct generates the enum conversion by constant name.
> 3. Leave `toVisitDto` and `toVisitsDto` unchanged. `status` maps by name automatically.
> Do not map `status` anywhere else. Do not set it again after the mapping call.
> Check the generated `VisitMapperImpl` and confirm `status` is set once per mapping method.

**Review checkpoint:** The generated mapper sets `status` to `SCHEDULED` when the request value is null, and to the matching enum constant otherwise. `toVisitDto` returns the stored status. No field is mapped twice.

---

## Step 9 — Keep the stored status on update; apply create default in both create endpoints

**Goal:** `updateVisit` leaves the status unchanged when the request status is null. Both create endpoints use the mapper default (AC3, AC4, AC5, AC10).
**Implements:** AC3, AC4, AC5, AC10
**Depends on:** Step 8

**Suggested prompt:**

> Edit F9 (`VisitRestControllerV1.java`), method `updateVisit`: after the `date` and `description` assignments, add `if (visitDto.getStatus() != null) { currentVisit.setStatus(visitMapper.toVisitStatus(visitDto.getStatus())); }`. Do not change the 404 or 204 behaviour.
> Check F10 (`OwnerRestControllerV1.java`), method `addVisitToOwner`: confirm it uses `visitMapper.toVisit(visitFieldsDto)` and that the Step 8 default covers it. Edit it only if the default does not apply.
> Check `addVisit` in F9: it uses `visitMapper.toVisit(visitDto)`, so the Step 8 default covers it. Do not add a second default.
> Do not change `deleteVisit` or `listVisits`.

**Review checkpoint:** A PUT without `status` keeps the stored value (AC5, AC10). A POST without `status` saves `SCHEDULED` through both create endpoints (AC3). Only one place sets the create default.

---

## Step 10 — Return 400 for an invalid status and nothing else

**Goal:** Invalid `status` values return 400 with a ProblemDetail body. Nothing is saved. Other malformed bodies are unchanged (AC6, AC11).
**Implements:** AC6, AC11
**Depends on:** Step 2, Step 3

**Suggested prompt:**

> Implement the option chosen in Step 2 in F11 (`ExceptionControllerAdvice.java`) only.
> - If Option 1 was chosen (existing mapping already gives 400 for unknown enum values), make no code change. Add a short note in the Step 10 review.
> - If Option 2 was chosen, add a targeted handler for `InvalidFormatException` whose target type is the generated `VisitStatusDto` enum. Return 400 with a ProblemDetail body that contains no internal message. Leave every other `HttpMessageNotReadableException` case unchanged.
> Do not change status codes for any other exception. Do not log request bodies.

**Review checkpoint:** `DONE`, `completed`, and `""` in `status` return 400 on POST and PUT (AC6, AC11). A visit is not created or changed on those requests. Malformed JSON and a body with an unrelated bad field return exactly what they returned before. Confirm this against the Step 1 baseline.

---

## Step 11 — Verify nested `status` in the v2 and owner responses

**Goal:** Confirm `status` appears on each visit under each pet in `/api/owners/{ownerId}`, `/api/v2/owners`, and `/api/v2/pets` (AC7).
**Implements:** AC7
**Depends on:** Step 3, Step 4, Step 7

**Suggested prompt:**

> Inspect F12 (`OwnerRestControllerV2.java`), F13 (`PetRestControllerV2.java`), and the owner and pet mappers they use. Confirm that nested visits are mapped through `VisitMapper` or a mapper that uses the same `Visit` → `VisitDto` mapping, so `status` is included by name. Do not edit anything unless a nested visit mapping copies fields by hand and drops `status`. If so, report it and propose the smallest change, but do not apply it without confirmation.
> Also confirm the owner v1 response (`GET /api/owners/{ownerId}`) uses the same nested mapping.

**Review checkpoint:** All three responses include `status` on nested visits. If a hand-written mapping drops it, that is fixed in this step, with the file ID added to the Impacted Files block.

---

## Step 12 — Persistence round-trip tests across all persistence profiles

**Goal:** Test that `status` is saved and read correctly on JDBC H2, JDBC HSQLDB, JPA, and Spring Data JPA (AC2, AC4, AC5, AC8).
**Implements:** AC2, AC4, AC5, AC8
**Depends on:** Step 6, Step 7, Step 9

**Suggested prompt:**

> In the existing `ClinicService*Tests` classes under F23 (JDBC H2/HSQLDB, JPA, Spring Data JPA), add tests that match each class's existing style:
> 1. A seeded visit returns `SCHEDULED` (AC2).
> 2. Saving a new visit with `SCHEDULED` default is read back as `SCHEDULED`. Use the default, not an explicit value (AC3 at persistence level).
> 3. Saving a new visit with `COMPLETED` is read back as `COMPLETED` (AC4).
> 4. Updating a stored visit to `CANCELLED` is read back as `CANCELLED`, and an update that keeps the existing status keeps it (AC5).
> Use synthetic values only. Do not log visit contents.
> Do not disable or remove existing tests.

**Review checkpoint:** Each persistence profile has a test for create default, explicit create, update, and seeded read. No test asserts only that a mock was called.

---

## Step 13 — Controller and mapper tests for the API cases

**Goal:** Cover every API-level case in AC3–AC7, AC10, and AC11.
**Implements:** AC3, AC4, AC5, AC6, AC7, AC10, AC11
**Depends on:** Step 8, Step 9, Step 10, Step 11

**Suggested prompt:**

> Add tests in F22 (mapper), F24 (v1 controllers), and F25 (v2 controllers), matching the existing style:
> 1. `VisitMapper`: null request status → `SCHEDULED`; `COMPLETED` → `COMPLETED`; `toVisitDto` includes the stored status.
> 2. `POST /api/visits` without `status` → 201 and `"status": "SCHEDULED"` (AC3).
> 3. `POST /api/visits` with `"status": "COMPLETED"` → 201 (AC4).
> 4. `PUT /api/visits/{id}` with `"status": "CANCELLED"` → 204; `PUT` without `status` keeps the stored value (AC5).
> 5. `POST` and `PUT` with `"status": "DONE"`, `"completed"`, and `""` → 400, and the service save is never called (AC6, AC11).
> 6. `POST` and `PUT` with `"status": null` → create saves `SCHEDULED`, update keeps the stored value, no 400 (AC10).
> 7. Malformed JSON and a body with an unrelated invalid field keep their existing status and body (AC11).
> 8. Owner v1 and v2 pet and owner responses include `status` on nested visits (AC7).
> Assert the 400 response has the ProblemDetail shape and no stack trace. Use only synthetic values.

**Review checkpoint:** Each case in this list has a test. The 400 tests verify that the save method was not called (no change persisted). The null-status and malformed-body tests pass against the baseline behaviour.

---

## Step 14 — Full test suite

**Goal:** Confirm the whole project passes (AC1, AC9).
**Implements:** AC1, AC9
**Depends on:** Step 12, Step 13

**Suggested prompt:**

> Run `./mvnw test` from the service directory. If it fails, report the failing test names and the first failure message in each. Do not weaken or skip any test to make it pass. Confirm `target/generated-sources/openapi` contains `VisitStatusDto` (or the name confirmed in Step 3).

**Review checkpoint:** `./mvnw test` passes (AC9). The generated enum exists with the three values (AC1). If any failure is a pre-existing one not related to visits, record it as pre-existing, not fixed here.

---

## Step 15 — Manual validation against the story's acceptance criteria

**Goal:** Check every AC against the running application, with the H2 profile and then the HSQLDB profile.
**Implements:** — (verification step, no AC)
**Depends on:** Step 14

**Suggested prompt:**

> Produce a checklist with one row per AC from AC1 to AC11, including CL-1 (AC10) and CL-2 (AC11). For each row, give the exact request (method, path, body, and auth) and the expected status and body fields. Include AC1 as a file check on `openapi.yml`. Do not run anything; give me the checklist.

| AC | Manual check | Expected |
|---|---|---|
| AC1 | Inspect `openapi.yml` | Enum has exactly three values; visit schema references it |
| AC2 | `GET /api/visits/1` | `"status": "SCHEDULED"` |
| AC3 | `POST /api/visits` without status | 201, `"status": "SCHEDULED"` |
| AC4 | `POST /api/visits` with COMPLETED, then GET | 201, then `"status": "COMPLETED"` |
| AC5 | PUT CANCELLED → GET; PUT without status → GET | 204, `CANCELLED`; then still `CANCELLED` |
| AC6 | POST and PUT with `DONE` | 400; GET shows the visit unchanged |
| AC7 | `GET /api/owners/{id}`, `/api/v2/owners`, `/api/v2/pets` | `status` on every nested visit |
| AC8 | Repeat AC2–AC5 on the H2, HSQLDB, and JPA profiles | Same results on each |
| AC9 | `./mvnw test` | Passes |
| AC10 | PUT and POST with `"status": null` | No 400; create = SCHEDULED; update unchanged |
| AC11 | `completed`, `""`, malformed JSON | 400 for the first two; malformed JSON unchanged |

**Review checkpoint:** Record pass, fail, or unclear per AC id. Any failure goes back to the step that caused it.

---

## Step 16 — Convention drift review

**Goal:** Check changed files against `.github/copilot-instructions.md` and the repo rules, without fixing anything automatically.
**Implements:** — (verification step, no AC)
**Depends on:** Step 14

**Suggested prompt:**

> Review every changed file against `.github/copilot-instructions.md`. Check:
> - Constructor injection only; no new field injection.
> - No JPA entity returned from a controller.
> - No hand-written generated classes or edits under `target/generated-sources/openapi`.
> - Spring `@Transactional` in new service code only.
> - Every `@PreAuthorize` guard is still present on each visit endpoint.
> - No visit payload, owner, pet, or visit data in any log statement.
> - Only invalid `status` values produce 400 (AC11).
> - Only comments that explain a non-obvious reason.
> List any drift with file and line. Do not fix anything.

**Review checkpoint:** The developer decides which drift items to fix. Nothing is changed automatically.

---

## Done criteria

Before opening a PR, confirm:
- [ ] AC1: `openapi.yml` defines `VisitStatus` with exactly `SCHEDULED`, `COMPLETED`, `CANCELLED`, and the visit schema includes `status`.
- [ ] AC2: GET of a seeded visit returns `"status": "SCHEDULED"`.
- [ ] AC3: POST without `status` returns 201 with `"status": "SCHEDULED"`.
- [ ] AC4: POST with `COMPLETED` returns 201, and a later GET returns `COMPLETED`.
- [ ] AC5: PUT with `CANCELLED` returns 204, and a later GET returns `CANCELLED`. PUT without `status` keeps the stored value.
- [ ] AC6: POST or PUT with `DONE` returns 400, and nothing is saved or changed.
- [ ] AC7: Nested visits in `/api/owners/{ownerId}`, `/api/v2/owners`, and `/api/v2/pets` include `status`.
- [ ] AC8: Status is saved and read correctly on JDBC H2, JDBC HSQLDB, JPA, and Spring Data JPA.
- [ ] AC9: `./mvnw test` passes.
- [ ] AC10: `"status": null` on create saves `SCHEDULED`; on update it is treated as absent. No 400.
- [ ] AC11: Only invalid values return 400. Other malformed bodies are unchanged on every endpoint.
- [ ] Seeded visits load with `SCHEDULED` in every profile that seeds visits.
- [ ] No Angular, security, pet, owner, vet, or specialty change beyond the nested visit `status`.
- [ ] No Docker, Kubernetes, or Postman file changed.

---

## Risks

- **Existing persistent databases:** `CREATE TABLE IF NOT EXISTS` (H2, MySQL, Postgres) does not add the new column to an existing table. Any non-ephemeral MySQL or Postgres database needs a manual `ALTER TABLE visits ADD COLUMN status ...`. This is out of scope for this story and is flagged for the team.
- **Generated enum name:** If it is not `VisitStatusDto`, every FQN reference in Steps 8, 10, and 13 must change. Caught in Step 3.
- **Null on create reaching the entity:** MapStruct sets an explicit null from the request. Step 8 uses `defaultExpression` to prevent this. If MapStruct does not generate the default, the JDBC insert will fail on the NOT NULL column. Step 14 catches this.
- **Empty string `""`:** Depends on Jackson enum handling. Step 2 and Step 13 cover it explicitly.
- **Unknown reserved word:** `status` is not reserved in these databases as far as known. Step 5 checks it.
- **Context file ACs:** The context file's AC numbering does not match the story. The plan follows the story. The context file's Section 9 open item (full enum values) is resolved by the story.

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-11-01T15:55:27
- phase: coding
- approved impacted files: ['src/main/java/org/springframework/samples/petclinic/mapper/VisitMapper.java', 'src/main/java/org/springframework/samples/petclinic/model/Visit.java', 'src/main/java/org/springframework/samples/petclinic/model/VisitStatus.java', 'src/main/java/org/springframework/samples/petclinic/repository/jdbc/', 'src/main/java/org/springframework/samples/petclinic/repository/jdbc/JdbcPetVisitExtractor.java', 'src/main/java/org/springframework/samples/petclinic/repository/jdbc/JdbcVisitRepositoryImpl.java', 'src/main/java/org/springframework/samples/petclinic/repository/jdbc/JdbcVisitRowMapper.java', 'src/main/java/org/springframework/samples/petclinic/rest/advice/ExceptionControllerAdvice.java', 'src/main/java/org/springframework/samples/petclinic/rest/controller/v1/OwnerRestControllerV1.java', 'src/main/java/org/springframework/samples/petclinic/rest/controller/v1/VisitRestControllerV1.java', 'src/main/java/org/springframework/samples/petclinic/rest/controller/v2/OwnerRestControllerV2.java', 'src/main/java/org/springframework/samples/petclinic/rest/controller/v2/PetRestControllerV2.java', 'src/main/resources/db/h2/data.sql', 'src/main/resources/db/h2/schema.sql', 'src/main/resources/db/hsqldb/data.sql', 'src/main/resources/db/hsqldb/schema.sql', 'src/main/resources/db/mysql/data.sql', 'src/main/resources/db/mysql/schema.sql', 'src/main/resources/db/postgres/data.sql', 'src/main/resources/db/postgres/schema.sql', 'src/main/resources/openapi.yml', 'src/test/java/org/springframework/samples/petclinic/mapper/', 'src/test/java/org/springframework/samples/petclinic/rest/controller/v1/', 'src/test/java/org/springframework/samples/petclinic/rest/controller/v2/', 'src/test/java/org/springframework/samples/petclinic/service/ClinicService*Tests']
- actually touched: ['src/main/java/org/springframework/samples/petclinic/mapper/VisitMapper.java', 'src/main/java/org/springframework/samples/petclinic/model/Visit.java', 'src/main/java/org/springframework/samples/petclinic/model/VisitStatus.java', 'src/main/java/org/springframework/samples/petclinic/repository/jdbc/JdbcOwnerRepositoryImpl.java', 'src/main/java/org/springframework/samples/petclinic/repository/jdbc/JdbcVisitRepositoryImpl.java', 'src/main/java/org/springframework/samples/petclinic/repository/jdbc/JdbcVisitRowMapper.java', 'src/main/java/org/springframework/samples/petclinic/rest/advice/ExceptionControllerAdvice.java', 'src/main/java/org/springframework/samples/petclinic/rest/controller/v1/VisitRestControllerV1.java', 'src/main/resources/db/h2/schema.sql', 'src/main/resources/db/hsqldb/data.sql', 'src/main/resources/db/hsqldb/schema.sql', 'src/main/resources/db/mysql/data.sql', 'src/main/resources/db/mysql/schema.sql', 'src/main/resources/db/postgres/schema.sql', 'src/main/resources/openapi.yml']
- ⚠ SCOPE ADDITION (touched, not in approved plan): ['src/main/java/org/springframework/samples/petclinic/repository/jdbc/JdbcOwnerRepositoryImpl.java']
  -> review this scope change before approving the coding phase.
- review status: APPROVED by human at 2026-11-01T15:55:27

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-11-01T15:55:56
- phase: coding
- approved impacted files: ['src/main/java/org/springframework/samples/petclinic/mapper/VisitMapper.java', 'src/main/java/org/springframework/samples/petclinic/model/Visit.java', 'src/main/java/org/springframework/samples/petclinic/model/VisitStatus.java', 'src/main/java/org/springframework/samples/petclinic/repository/jdbc/', 'src/main/java/org/springframework/samples/petclinic/repository/jdbc/JdbcPetVisitExtractor.java', 'src/main/java/org/springframework/samples/petclinic/repository/jdbc/JdbcVisitRepositoryImpl.java', 'src/main/java/org/springframework/samples/petclinic/repository/jdbc/JdbcVisitRowMapper.java', 'src/main/java/org/springframework/samples/petclinic/rest/advice/ExceptionControllerAdvice.java', 'src/main/java/org/springframework/samples/petclinic/rest/controller/v1/OwnerRestControllerV1.java', 'src/main/java/org/springframework/samples/petclinic/rest/controller/v1/VisitRestControllerV1.java', 'src/main/java/org/springframework/samples/petclinic/rest/controller/v2/OwnerRestControllerV2.java', 'src/main/java/org/springframework/samples/petclinic/rest/controller/v2/PetRestControllerV2.java', 'src/main/resources/db/h2/data.sql', 'src/main/resources/db/h2/schema.sql', 'src/main/resources/db/hsqldb/data.sql', 'src/main/resources/db/hsqldb/schema.sql', 'src/main/resources/db/mysql/data.sql', 'src/main/resources/db/mysql/schema.sql', 'src/main/resources/db/postgres/data.sql', 'src/main/resources/db/postgres/schema.sql', 'src/main/resources/openapi.yml', 'src/test/java/org/springframework/samples/petclinic/mapper/', 'src/test/java/org/springframework/samples/petclinic/rest/controller/v1/', 'src/test/java/org/springframework/samples/petclinic/rest/controller/v2/', 'src/test/java/org/springframework/samples/petclinic/service/ClinicService*Tests']
- actually touched: ['src/main/resources/db/hsqldb/schema.sql']
- scope: matches approved plan (no additions)
- review status: APPROVED by human at 2026-11-01T15:55:56
