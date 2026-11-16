# Plan for: Trim spaces around a pet type name

**Source:** .github/story-context-files/pet-type-trimming-context-261116-070426.md + .github/copilot-instructions.md
**Stack:** backend (Spring Boot 4 MVC, MapStruct, JUnit 5 / MockMvc)
**Total steps:** 4
**Unresolved clarifications:** None

---

## Acceptance criteria coverage

| AC | Criterion (abbreviated) | Covered by |
|---|---|---|
| AC1 | POST with `"  hamster  "` returns 201, name `"hamster"` | Step 2, Step 4 |
| AC2 | PUT with `"  dog "` returns 204, saved name `"dog"` | Step 3, Step 4 |
| AC3 | POST with `"guinea pig"` returns 201, name `"guinea pig"` | Step 2, Step 4 |
| AC4 | Existing controller tests pass, plus new tests for AC1–AC3 | Step 4 (verification) |
| AC5 | Names with no surrounding spaces saved exactly as received | Step 2, Step 3 (trim is a no-op); asserted in Step 4 |
| AC6 | Null name keeps existing behaviour; no new validation or error | No step required — no code added for null; existing `testCreatePetTypeError` and Step 4 run verify it |

Note: AC5 and AC6 are in the context file but were not listed in the story text. They are carried into the plan unchanged.

---

## Before you execute any step

1. Single-file production change in the controller; tests in one existing test class. No mapper, DTO, OpenAPI, model, service or repository change.
2. Trimming is done in the controller, after the DTO is mapped and before `clinicService.savePetType(...)`. The mapper is not changed.
3. Null-safety: a null name must pass through unchanged (no NPE).

---

## Pre-flight

1. **Stack assumption:** Backend is Spring Boot with Spring MVC controllers implementing the generated `PettypesApi` interface, MapStruct mappers, and `ClinicService` persistence. No frontend change.
2. **Behaviour preservation:**
   - Null `name` on create still returns 400 (`testCreatePetTypeError`).
   - Empty `name` on update still returns 400 (`testUpdatePetTypeError`).
   - Update still returns 204 with no body; create still returns 201 with a `Location` header.
   - `VET_ADMIN` role is still required for create and update.
   - Spaces inside a name are kept.
3. **Non-functional handling:** None in the story. No performance or load requirement applies.
4. **Assumed criteria:** No assumed criteria. AC5 and AC6 come from the context file.
5. **Integration pattern:** No new external integration. Not applicable.
6. **Open risk (not in the story, not planned):** A name made only of spaces (e.g. `"   "`) passes the existing `@Size(min=1)` check before the controller runs, then becomes `""` after trimming. The story says to add no validation, so this plan does not change it. Confirm with the story owner before release.

If any of these assumptions are wrong, stop and revise the context file or the plan before proceeding.

---

## Impacted Files

| ID | Path | Role |
|----|------|------|
| F1 | src/main/java/org/springframework/samples/petclinic/rest/controller/v1/PetTypeRestControllerV1.java | Controller; `addPetType` and `updatePetType` gain name trimming |
| F2 | src/test/java/org/springframework/samples/petclinic/rest/controller/PetTypeRestControllerV1Tests.java | Existing MockMvc tests; new tests for AC1–AC3 and AC5 |

Files confirmed NOT impacted: `src/main/resources/openapi.yml`, `mapper/PetTypeMapper.java`, `model/PetType.java`, repository classes, DB schema scripts, other controllers, Angular frontend.

---

## External Classes

None.

All classes used are already in this repo or the JDK/libraries the changed files already import (`PetType`, `PetTypeDto`, `PetTypeFieldsDto`, `ClinicService`, Mockito `ArgumentCaptor`, Spring test classes already used in the test file). Generated DTOs are referenced only by their generated names `PetTypeDto` and `PetTypeFieldsDto` from `org.springframework.samples.petclinic.rest.dto`, which already exist in the build.

---

## Signature changes

None — no existing public or interface method changes signature.

---

## Contract usage

None — this change reads no new or changed contract from another repo. The OpenAPI contract is unchanged.

---

## Step 1 — Confirm the target methods and imports

**Goal:** Read `PetTypeRestControllerV1.java` (F1) and `PetTypeRestControllerV1Tests.java` (F2) to confirm the exact lines to change and existing imports before editing.
**Implements:** — (enabling step, no AC)
**Depends on:** —

**Suggested prompt:**

> Read F1 and F2. Confirm: (a) the `addPetType` method maps `PetTypeFieldsDto` to `PetType` via `petTypeMapper.toPetType(...)` and then calls `clinicService.savePetType(type)`; (b) `updatePetType` calls `currentPetType.setName(petTypeDto.getName())` before `savePetType`; (c) which test imports already exist (`ArgumentCaptor`, `Mockito.verify`, `jsonPath`). Do not edit any file in this step.

**Review checkpoint:** Developer confirms the two methods and the existing test imports match the description above. If the code differs, stop and update this plan before continuing.

---

## Step 2 — Trim the name on create (F1)

**Goal:** In `addPetType`, trim leading and trailing spaces from the mapped name before saving. Covers AC1, AC3, AC5.
**Implements:** AC1, AC3, AC5
**Depends on:** Step 1

**Target change in F1 (text description, do not apply in planning phase):**

- Add a private static helper in the same controller:
  ```java
  private static String trimName(String name) {
      return name == null ? null : name.strip();
  }
  ```
  (`strip()` is preferred over `trim()` so Unicode whitespace is handled; the story says "spaces", so either is acceptable — choose `trim()` if the team wants strictly ASCII behaviour.)
- In `addPetType`, after `final PetType type = petTypeMapper.toPetType(petTypeFieldsDto);` add:
  ```java
  type.setName(trimName(type.getName()));
  ```
  before `this.clinicService.savePetType(type);`. The rest of the method is unchanged.

**Suggested prompt:**

> Edit F1 only. In `addPetType`, after the mapper call and before `clinicService.savePetType`, set the name to its trimmed value using a null-safe private static helper `trimName`. Do not change the `@PreAuthorize`, the Location header, or the response status. Do not add validation. Do not modify F2 yet.

**Review checkpoint:** Confirm: only `addPetType` and the new private helper changed; a null name passes through as null; no other method changed.

---

## Step 3 — Trim the name on rename (F1)

**Goal:** In `updatePetType`, trim the submitted name before assigning it to the existing pet type. Covers AC2, AC5.
**Implements:** AC2, AC5
**Depends on:** Step 2 (reuses the `trimName` helper)

**Target change in F1 (text description):**

- Change `currentPetType.setName(petTypeDto.getName());` to
  ```java
  currentPetType.setName(trimName(petTypeDto.getName()));
  ```
  Nothing else in `updatePetType` changes. It still returns 404 when the pet type is missing and 204 on success.

**Suggested prompt:**

> Edit F1 only. In `updatePetType`, change the `setName` call to use `trimName(petTypeDto.getName())`. Do not change the not-found branch, the 204 response, or the `@PreAuthorize`. Do not modify F2 yet.

**Review checkpoint:** Confirm: only the single `setName` line in `updatePetType` changed; the `trimName` helper from Step 2 is reused, not duplicated.

---

## Step 4 — Add and run controller tests (F2)

**Goal:** Add MockMvc tests for AC1, AC2, AC3, AC5 and run the whole class. Covers AC4.
**Implements:** AC1, AC2, AC3, AC4, AC5
**Depends on:** Step 3

**Tests to add to F2 (text description; each is one test method, `void`, package-private, JUnit 5, `@WithMockUser(roles="VET_ADMIN")`):**

1. `testCreatePetTypeTrimsSurroundingSpaces` (AC1) — POST JSON with `"name": "  hamster  "`; expect 201, `jsonPath("$.name").value("hamster")`.
2. `testUpdatePetTypeTrimsSurroundingSpaces` (AC2) — stub `clinicService.findPetTypeById(2)` to return the dog pet type; PUT `/api/pettypes/2` with `"name": "  dog "`; expect 204; use `ArgumentCaptor<PetType>` on `clinicService.savePetType(...)` and assert the captured name equals `"dog"`.
3. `testCreatePetTypeKeepsInternalSpaces` (AC3) — POST with `"name": "guinea pig"`; expect 201 and `jsonPath("$.name").value("guinea pig")`.
4. `testCreatePetTypeWithoutSurroundingSpacesUnchanged` (AC5) — POST with `"name": "snake"`; expect 201 and `jsonPath("$.name").value("snake")`.

**Notes for the tests:**
- Build the JSON body by hand (string literal) rather than from `petTypeMapper`, so the spaces are preserved and not normalised by the test setup.
- Existing `testCreatePetTypeError` and `testUpdatePetTypeError` must remain unchanged (they cover AC6).
- Do not change the existing tests' assertions.
- No `Thread.sleep`, no real DB, no static shared state.

**Suggested prompt:**

> Edit F2 only. Add the four test methods listed in Step 4 to `PetTypeRestControllerV1Tests`, following the style of the existing tests in the class. Build request bodies as literal JSON strings so the leading and trailing spaces are preserved. Use `ArgumentCaptor` for the update case. Do not modify any existing test method. Then run the test class: `./mvnw -Dtest=PetTypeRestControllerV1Tests test`.

**Review checkpoint:**
- All existing tests in `PetTypeRestControllerV1Tests` still pass, including `testCreatePetTypeError` and `testUpdatePetTypeError` (AC4, AC6).
- The four new tests pass (AC1, AC2, AC3, AC5).
- No existing test method body was edited.
- If a test fails, fix the controller (F1), not the test expectations.

---

## Done criteria

Before opening a PR, confirm:
- [ ] AC1: `POST /api/pettypes` with `"  hamster  "` returns 201 and name `"hamster"` (test added and passing).
- [ ] AC2: `PUT /api/pettypes/2` with `"  dog "` returns 204 and the saved name is `"dog"` (test added and passing).
- [ ] AC3: `POST /api/pettypes` with `"guinea pig"` returns 201 and name `"guinea pig"` (test added and passing).
- [ ] AC4: All tests in `PetTypeRestControllerV1Tests` pass.
- [ ] AC5: Names without surrounding spaces are saved exactly as sent (test added and passing).
- [ ] AC6: Null and empty name behaviour unchanged; no new validation or error responses added.
- [ ] `src/main/resources/openapi.yml` is unchanged.
- [ ] No changes outside F1 and F2.
- [ ] Open risk in Pre-flight (whitespace-only name becomes empty string) is raised with the story owner.

---

## Validation (manual, no Copilot)

Walk each AC against the running app:
- AC1: `curl -u <vet-admin> -X POST /api/pettypes -H 'Content-Type: application/json' -d '{"name":"  hamster  "}'` → 201, body name `hamster`.
- AC2: `curl -X PUT /api/pettypes/{id}` with `{"name":"  dog "}` → 204; `GET /api/pettypes/{id}` → name `dog`.
- AC3: POST `{"name":"guinea pig"}` → 201, name `guinea pig`.
- AC5: POST `{"name":"snake"}` → 201, name `snake`.
- AC6: POST `{"name":null}` → 400 as before.

Record pass / fail / unclear per AC id.

---

## Convention drift check (after code steps)

Copilot reviews F1 and F2 against `.github/copilot-instructions.md` and lists any drift for the developer to decide. It does not auto-fix. Points to check: constructor injection unchanged, no field injection, controller stays thin (trimming is a one-line presentation step, not business logic — confirm this is acceptable or move it to the service layer if the team prefers), Javadoc on the new helper only if its purpose is not obvious.

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-11-16T07:07:13
- phase: coding
- approved impacted files: ['src/main/java/org/springframework/samples/petclinic/rest/controller/v1/PetTypeRestControllerV1.java', 'src/test/java/org/springframework/samples/petclinic/rest/controller/PetTypeRestControllerV1Tests.java']
- actually touched: ['src/main/java/org/springframework/samples/petclinic/rest/controller/v1/PetTypeRestControllerV1.java']
- scope: matches approved plan (no additions)
- review status: APPROVED by human at 2026-11-16T07:07:19
