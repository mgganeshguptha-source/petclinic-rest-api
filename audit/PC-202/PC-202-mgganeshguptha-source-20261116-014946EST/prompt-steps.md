# Plan for: Trim spaces around a pet type name

**Source:** .github/story-context-files/pet-type-name-trim-context-261116-065053.md + .github/copilot-instructions.md
**Stack:** backend (Spring Boot 4.1.1, Maven, MapStruct, OpenAPI-generated DTOs)
**Total steps:** 8
**Unresolved clarifications:** None in the context file. One decision point is recorded in Pre-flight (whitespace-only names).

---

## Acceptance criteria coverage

| AC | Criterion (abbreviated) | Covered by |
|---|---|---|
| AC-1 | POST `"  hamster  "` returns 201 with name `"hamster"` | Step 3, Step 4 |
| AC-2 | PUT `"  dog "` saves `"dog"` and returns 204 | Step 2, Step 5 |
| AC-3 | POST `"guinea pig"` returns 201 with `"guinea pig"` | Step 3, Step 4 |
| AC-4 | Controller test suite includes regression tests for AC-1 to AC-3 | Step 4, Step 5 |
| AC-5 | Existing pet type controller tests continue to pass | Step 6 |
| AC-6 | Name with no surrounding spaces is saved unchanged | Step 2, Step 3, Step 4 |
| AC-7 | Null name keeps current method-specific behaviour, no new validation | Step 2, Step 3 (null-safe helper). Existing `testCreatePetTypeError` (null → 400) and `testUpdatePetTypeError` (empty → 400) verify it in Step 6. |

Every AC id above is taken unchanged from the context file. No AC is `[ASSUMED]` or `[WITHDRAWN]`.

---

## Before you execute any step

1. Keep `.github/story-context-files/pet-type-name-trim-context-261116-065053.md` in context throughout.
2. **Blocker (read first):** `src/main/java/org/springframework/samples/petclinic/model/PetType.java` contains a deliberate compile error (`public int deliberateCompileErrorForHarnessTest()` returns a String). The project does not compile, so Step 6 cannot run. This file is out of scope for this story and this plan does not edit it. Resolve it separately (remove that method) before Step 6.
3. Steps 2 to 5 edit only F1 and F2 below. Do not touch the mapper, model, OpenAPI file, or other controllers.

---

## Pre-flight

The plan assumes:

1. **Stack:** Spring Boot with Spring MVC controllers implementing OpenAPI-generated interfaces (`PettypesApi`), MapStruct mapper `PetTypeMapper`, and generated DTOs `PetTypeDto` (schema `PetType`) and `PetTypeFieldsDto` (schema `PetTypeFields`). The Angular front end is out of scope.
2. **Behaviour preservation:**
   - `POST /api/pettypes` still returns 201 with a `Location` header and the mapped DTO.
   - `PUT /api/pettypes/{petTypeId}` still returns 204, and 404 when the pet type is missing.
   - Null and empty names keep the existing 400 responses (bean validation `minLength: 1` / `required` runs at binding, before the controller).
   - `@PreAuthorize("hasRole(@roles.VET_ADMIN)")` on add and update is unchanged.
3. **Non-functional handling:** None required by the story.
4. **Assumed criteria:** No assumed criteria. All AC-1 to AC-7 were requested by the story.
5. **Trim definition:** Use `String.trim()`. It removes leading and trailing spaces and other control characters at or below U+0020. Tabs and newlines at the ends are also removed. Internal spaces are kept.
6. **Where the trim happens:** Only in `PetTypeRestControllerV1`.
   - Add: trim the request DTO's name before `petTypeMapper.toPetType(...)` is called, so the mapper sets the field once (per the "map each field in one place" rule).
   - Update: trim at the `setName` call that already exists. The update path does not use the mapper for the name.
7. **Generated setter:** `PetTypeFieldsDto.setName(String)` is assumed to exist, because the generator produces standard setters (the repo's `pom.xml` uses the `spring` generator with `openApiNullable=false`). Step 1 confirms this from `target/generated-sources` after a build. If the setter does not exist, stop and report. Do not hand-write the class.
8. **Whitespace-only names (decision for developer):** Validation runs before the controller. A name such as `"   "` passes `minLength: 1`, is trimmed to `""`, and is saved as an empty string. The story forbids new validation, so this plan does not guard against it. Confirm this is acceptable, or raise it as a follow-up story.
9. **Integration:** This story adds no new external integration. The "follow this repo's existing integration" rule does not apply.

If any of these assumptions are wrong, stop and revise the context file or the plan before proceeding.

---

## External Classes

None.

The planned code uses only classes the changed controller and test already import: `PetTypeDto`, `PetTypeFieldsDto` (generated from `src/main/resources/openapi.yml`), `PetType`, `PetTypeMapper`, `ClinicService`, and the JUnit, Mockito, Spring MVC and Jackson test classes already in the test file.

---

## Signature changes

None — no existing public or interface method changes signature. One new `private static` helper is added to F1 (see Step 2).

---

## Contract usage

None — this change reads no new or changed contract from another repo. The OpenAPI contract is unchanged.

---

## Impacted Files

| ID | Path | Role |
|----|------|------|
| F1 | `src/main/java/org/springframework/samples/petclinic/rest/controller/v1/PetTypeRestControllerV1.java` | Controller. Add and update methods trim the name before save. |
| F2 | `src/test/java/org/springframework/samples/petclinic/rest/controller/PetTypeRestControllerV1Tests.java` | Controller test class. Adds regression tests for AC-1 to AC-3 and AC-6. |

Reference only (read, not edited):
- `src/main/java/org/springframework/samples/petclinic/mapper/PetTypeMapper.java`: unchanged.
- `src/main/java/org/springframework/samples/petclinic/model/PetType.java`: blocker (see Before you execute). Not edited by this plan.
- `src/main/resources/openapi.yml`: unchanged (`PetTypeFields` at about line 2309).

---

## Step 1 — Confirm file set, generated setter, and build blocker

**Goal:** Confirm F1 and F2 are the only files to change, that the generated `PetTypeFieldsDto` has `setName(String)`, and record the `PetType.java` compile error as a blocker.
**Implements:** — (enabling step, no AC)
**Depends on:** —

**Suggested prompt:**

> Do not modify any files. Confirm that F1 (`PetTypeRestControllerV1.java`) and F2 (`PetTypeRestControllerV1Tests.java`) are the only files needed for this story. Check whether `target/generated-sources` exists. If it does, open the generated `PetTypeFieldsDto` and report whether it has `setName(String)` and `getName()`. If it does not exist, report that it must be generated before Step 3 can be verified. Also confirm that `PetType.java` contains the deliberate compile error `deliberateCompileErrorForHarnessTest`, and report it as a blocker for Step 6. Do not fix it.

**Review checkpoint:** Record F1 and F2 in the Impacted Files table (already filled). Confirm the setter result. If `setName` is missing on `PetTypeFieldsDto`, stop and revise Step 3 before continuing. Confirm the compile-error blocker is noted and will be resolved separately before Step 6.

---

## Step 2 — Add the trim helper and apply it to the rename path

**Goal:** Add a null-safe private helper in F1 and use it at the rename `setName` call, so rename trims names (AC-2) and leaves null and unpadded names unchanged (AC-6, AC-7).
**Implements:** AC-2, AC-6, AC-7
**Depends on:** Step 1

**Suggested prompt:**

> Edit F1 (`PetTypeRestControllerV1.java`). Do not change the add method yet. Do not change any test file.
>
> 1. Add this private static helper to the class, below the constructor:
>
> ```java
> private static String trimName(String name) {
>     return name == null ? null : name.trim();
> }
> ```
>
> 2. In `updatePetType`, change only the `setName` line:
>
> Before:
> ```java
> currentPetType.setName(petTypeDto.getName());
> ```
> After:
> ```java
> currentPetType.setName(trimName(petTypeDto.getName()));
> ```
>
> Do not change the 404 check, the save call, the response, or the `@PreAuthorize` annotation. Do not add validation.

**Review checkpoint:** Confirm the diff touches only the helper and the one `setName` line in `updatePetType`. Confirm null input returns null, so the existing 400 behaviour from bean validation is unchanged. If the diff touches anything else, revert it.

---

## Step 3 — Trim the name before mapping on the create path

**Goal:** Trim the request DTO's name before `petTypeMapper.toPetType(...)` runs in `addPetType`, so the saved entity and the 201 response both carry the trimmed name (AC-1, AC-3, AC-6, AC-7).
**Implements:** AC-1, AC-3, AC-6, AC-7
**Depends on:** Step 2

**Suggested prompt:**

> Edit F1 (`PetTypeRestControllerV1.java`), `addPetType` method only. Do not change the update method again. Do not change tests.
>
> Before:
> ```java
> HttpHeaders headers = new HttpHeaders();
> final PetType type = petTypeMapper.toPetType(petTypeFieldsDto);
> ```
> After:
> ```java
> HttpHeaders headers = new HttpHeaders();
> petTypeFieldsDto.setName(trimName(petTypeFieldsDto.getName()));
> final PetType type = petTypeMapper.toPetType(petTypeFieldsDto);
> ```
>
> Do not call `setName` on `type` after mapping, because the mapper already sets the name (one place per field). Keep the save, Location header, and 201 response unchanged.

**Review checkpoint:** Confirm the trim happens before `petTypeMapper.toPetType` and that there is no second `setName` after mapping. Confirm the 201 response still uses the mapped `type`, so the response name is the trimmed name. If `setName` does not exist on `PetTypeFieldsDto` (Step 1), stop and report.

---

## Step 4 — Add create regression tests (AC-1, AC-3, AC-6, AC-4 part 1)

**Goal:** Add focused tests in F2 for create trimming, an internal-space name, and an unpadded name, each checking the status, the response name, and the saved entity name.
**Implements:** AC-1, AC-3, AC-6, AC-4
**Depends on:** Step 3

**Suggested prompt:**

> Edit F2 (`PetTypeRestControllerV1Tests.java`). Add tests after `testCreatePetTypeError`. Follow the existing style of the file: JUnit 5 `@Test`, `@WithMockUser(roles="VET_ADMIN")`, `MockMvc` with `post("/api/pettypes")`, `ObjectMapper`, and package-private test methods. Do not change existing tests. Do not change production code.
>
> Add these tests:
>
> 1. `testCreatePetTypeTrimsSurroundingSpaces` (AC-1): build a request body with name `"  hamster  "`. Use `org.mockito.ArgumentCaptor<PetType>` with `verify(clinicService).savePetType(captor.capture())`. Assert `status().isCreated()`, `jsonPath("$.name").value("hamster")`, and `captor.getValue().getName()` equals `"hamster"`. Build the body as JSON from a `PetTypeFieldsDto` or a map, with the name set to `"  hamster  "`. Do not use `petTypeMapper` to set the name on the body, because the mapper is not the code under test.
> 2. `testCreatePetTypeKeepsInternalSpaces` (AC-3): name `"guinea pig"`. Assert 201, `jsonPath("$.name").value("guinea pig")`, and the captured saved name is `"guinea pig"`.
> 3. `testCreatePetTypeWithoutSurroundingSpacesIsUnchanged` (AC-6): name `"cat"`. Assert 201 and the captured saved name is `"cat"`.
>
> Use `given(...)` or `willAnswer` only if needed. `savePetType` is void on the mocked service, so do not stub it with `thenReturn`. Each test asserts one behaviour.

**Review checkpoint:** Run only the create tests (`-Dtest=PetTypeRestControllerV1Tests#testCreatePetType*`) after Step 6's blocker is resolved. Each new test asserts both the response name and the captured saved name. Tests must fail if the trim is removed from Step 3.

---

## Step 5 — Add update regression test (AC-2, AC-4 part 2)

**Goal:** Add a test in F2 for rename trimming: PUT with `"  dog "` returns 204 and the saved entity name is `"dog"`.
**Implements:** AC-2, AC-4
**Depends on:** Step 2

**Suggested prompt:**

> Edit F2 (`PetTypeRestControllerV1Tests.java`). Add `testUpdatePetTypeTrimsSurroundingSpaces` after `testUpdatePetTypeError`. Follow the existing update test pattern: `given(this.clinicService.findPetTypeById(2)).willReturn(petTypes.get(1));`, `put("/api/pettypes/2")`, `VET_ADMIN`, and JSON body. Build the body with a `PetTypeDto` or map whose name is `"  dog "`. Do not reuse `petTypes.get(1)` as the body source, because it is the entity returned by the mock and its name is set in the test. Use `ArgumentCaptor<PetType>` with `verify(clinicService).savePetType(captor.capture())`. Assert `status().isNoContent()` and `captor.getValue().getName()` equals `"dog"`. Do not change other tests.

**Review checkpoint:** The test fails if `trimName` is removed from the update path in Step 2, and passes with it. Confirm the captured name is `"dog"` and not `"  dog "`.

---

## Step 6 — Run the pet type controller test suite (AC-5, AC-4, AC-7 check)

**Goal:** Run the full `PetTypeRestControllerV1Tests` class, including the new tests, and confirm the existing tests still pass, including the null and empty 400 cases.
**Implements:** AC-5, AC-4
**Depends on:** Step 4, Step 5

**Suggested prompt:**

> Before running: confirm the deliberate compile error in `PetType.java` (`deliberateCompileErrorForHarnessTest`) has been removed by its owner. Do not edit `PetType.java` as part of this story. If it is still present, stop and report it.
>
> Run `mvn -Dtest=PetTypeRestControllerV1Tests test` from the repo root. Report the pass/fail count for the whole class. Confirm that `testCreatePetTypeError` (null name, expects 400) and `testUpdatePetTypeError` (empty name, expects 400) still pass, which verifies AC-7.

**Review checkpoint:** All tests in `PetTypeRestControllerV1Tests` pass, including the new ones and the existing ones. If an existing test fails, loop back to Step 2 or Step 3 and fix the production code. Do not weaken or delete the existing test.

---

## Step 7 — Validation against acceptance criteria (manual)

**Goal:** Confirm each AC by id against the running application. No Copilot involvement.
**Implements:** — (verification step, no AC)
**Depends on:** Step 6

**Suggested prompt:**

> Do not run code. Produce a verification checklist with one row per AC id (AC-1 to AC-7), giving the request and expected response for each. For AC-6 and AC-7, include the null-name and unpadded-name requests.

**Review checkpoint:** Work the checklist against the running app (start with `mvn spring-boot:run`, then call the endpoints with curl using a VET_ADMIN user). Record pass/fail/unclear per AC id. Specifically check that a name of `"   "` (whitespace only) behaves as described in Pre-flight item 8, and log the result for the developer.

---

## Step 8 — Convention drift review

**Goal:** Check changed files against `.github/copilot-instructions.md` and the Java conventions. List drift for the developer to decide; do not auto-fix.
**Implements:** — (verification step, no AC)
**Depends on:** Step 7

**Suggested prompt:**

> Review the diffs in F1 and F2 only against `.github/copilot-instructions.md` and the repo's Java conventions. Check: constructor injection unchanged, `@PreAuthorize` still present on add and update, no validation added, no JPA entity exposed, no comments except where clarification is needed, helper is `private static`, and no unused imports. List any drift. Do not fix it.

**Review checkpoint:** Decide on each drift item. Fix only the items you accept, and re-run Step 6.

---

## Done criteria

Before opening a PR, confirm:
- AC-1: `POST /api/pettypes` with `"  hamster  "` returns 201, response name `"hamster"` (Step 4, Step 7).
- AC-2: `PUT /api/pettypes/{petTypeId}` with `"  dog "` returns 204 and saves `"dog"` (Step 5, Step 7).
- AC-3: `POST /api/pettypes` with `"guinea pig"` returns 201, name `"guinea pig"` (Step 4, Step 7).
- AC-4: `PetTypeRestControllerV1Tests` includes regression tests for AC-1 to AC-3 and AC-6 (Steps 4 and 5).
- AC-5: all existing tests in `PetTypeRestControllerV1Tests` pass (Step 6).
- AC-6: an unpadded name is saved unchanged (Step 4, Step 7).
- AC-7: null name keeps existing behaviour; no new validation or error response (Step 6, Step 7).
- `openapi.yml`, model, mapper, repository, schema, other controllers, and the Angular front end are unchanged.
- `PetType.java` compile error is resolved by its owner, separately from this story.
- Whitespace-only name behaviour has been confirmed by the developer (Pre-flight item 8).

## --- EXECUTION RECORD (appended by harness) ---
- timestamp: 2026-11-16T06:54:27
- phase: coding
- approved impacted files: ['src/main/java/org/springframework/samples/petclinic/rest/controller/v1/PetTypeRestControllerV1.java', 'src/test/java/org/springframework/samples/petclinic/rest/controller/PetTypeRestControllerV1Tests.java']
- actually touched: ['src/main/java/org/springframework/samples/petclinic/rest/controller/v1/PetTypeRestControllerV1.java']
- scope: matches approved plan (no additions)
- review status: APPROVED by human at 2026-11-16T06:54:27
