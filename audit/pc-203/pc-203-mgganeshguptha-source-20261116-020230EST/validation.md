# Validation — Trim spaces around a pet type name

**Source:** pet-type-trimming-context-261116-070426.md
**Criteria checked:** 6  (met 5 · not met 1 · unverifiable 0)

**VERDICT: CHANGES_REQUESTED**

## Per-criterion verdicts

### AC1 — MET
WHEN `POST /api/pettypes` receives `"name": "  hamster  "`, THE pet type endpoint SHALL return status 201 with `"name": "hamster"` in the created pet type.
**Evidence:** `PetTypeRestControllerV1.addPetType` maps the request via `petTypeMapper.toPetType(petTypeFieldsDto)`, then calls `type.setName(trimName(type.getName()))` before `clinicService.savePetType(type)`. `trimName` (private static helper, same class) returns `name.strip()` for a non-null input, which removes leading and trailing whitespace from `"  hamster  "` to produce `"hamster"`. The response is built from the same mutated `type` object via `petTypeMapper.toPetTypeDto(type)` and returned with `HttpStatus.CREATED` (201), so the returned DTO carries the trimmed name. `PetTypeMapper.toPetTypeDto`/`toPetType` are plain MapStruct field mappings with no further name transformation (verified in `PetTypeMapper.java` — no `@Mapping` targeting `name`), so no other code path can reintroduce the untrimmed value.

### AC2 — MET
WHEN `PUT /api/pettypes/{petTypeId}` receives `"name": "  dog "`, THE pet type endpoint SHALL save the name `"dog"` and return status 204.
**Evidence:** `PetTypeRestControllerV1.updatePetType` calls `currentPetType.setName(trimName(petTypeDto.getName()))` before `clinicService.savePetType(currentPetType)`, and returns `HttpStatus.NO_CONTENT` (204) unconditionally on the found-entity path. `trimName("  dog ")` strips both the leading two spaces and the trailing one space to yield `"dog"`, which is what gets passed to `savePetType`.

### AC3 — MET
WHEN `POST /api/pettypes` receives `"name": "guinea pig"`, THE pet type endpoint SHALL return status 201 with the name `"guinea pig"`.
**Evidence:** `trimName` uses `String.strip()`, which only removes leading/trailing whitespace and leaves internal whitespace untouched. For input `"guinea pig"` (no surrounding spaces), `strip()` returns the identical string `"guinea pig"`, and the 201/created-DTO flow is the same `addPetType` path cited for AC1.

### AC4 — NOT_MET
WHEN the existing pet type controller test suite is run with tests for AC1 through AC3, THE test suite SHALL pass.
**Gap:** The criterion requires new tests covering AC1–AC3 to be added to the test suite and pass. `src/test/java/org/springframework/samples/petclinic/rest/controller/PetTypeRestControllerV1Tests.java` was inspected in full: it still contains only the pre-existing tests (`testGetPetTypeSuccessAsOwnerAdmin`, `testGetPetTypeSuccessAsVetAdmin`, `testGetPetTypeNotFound`, `testGetAllPetTypesSuccessAsOwnerAdmin`, `testGetAllPetTypesSuccessAsVetAdmin`, `testGetAllPetTypesNotFound`, `testCreatePetTypeSuccess`, `testCreatePetTypeError`, `testUpdatePetTypeSuccess`, `testUpdatePetTypeError`, `testDeletePetTypeSuccess`, `testDeletePetTypeError`). None of these assert on a leading/trailing-space input or a trimmed-name output (`"hamster"`, `"  dog "`/`"dog"`, or `"guinea pig"` do not appear anywhere in the file). The pre-existing tests happening to still pass (confirmed by the build gate) only shows the old behavior is undisturbed — it does not satisfy "with new tests for AC1 to AC3", which this file does not contain.
**Where:** `src/test/java/org/springframework/samples/petclinic/rest/controller/PetTypeRestControllerV1Tests.java` — no new test methods were added.

### AC5 — MET
WHEN either write operation receives a name with no spaces at its start or end, THE pet type endpoint SHALL save the name exactly as received.
**Evidence:** `trimName` is `name == null ? null : name.strip()`. `String.strip()` is idempotent on a string with no leading/trailing whitespace — it returns an equal string. Both `addPetType` and `updatePetType` route every name through this same helper before `savePetType`, so a name already free of surrounding spaces (e.g. the existing tests' `"cat"`, `"dog I"`) is saved unchanged, consistent with the existing `testCreatePetTypeSuccess`/`testUpdatePetTypeSuccess` tests still passing per the build gate.

### AC6 — MET
WHEN either write operation receives a null name, THE pet type endpoint SHALL retain the existing null-name behavior without adding validation or a new error response.
**Evidence:** `trimName(null)` explicitly short-circuits to `null` rather than calling `.strip()` (which would NPE), so a null name is passed through to `clinicService.savePetType` unchanged, exactly as before this change (previously `type.setName(type.getName())`/`currentPetType.setName(petTypeDto.getName())` passed the null straight through too). No new validation annotation or `@ControllerAdvice`/exception-handling code was added in `PetTypeRestControllerV1.java` — the class's only changes are the two `trimName` call sites and the private helper itself. The existing `testCreatePetTypeError` (null name) and `testUpdatePetTypeError` (empty name) tests, which assert `400 Bad Request`, are unmodified and — per the build gate — still pass, confirming the pre-existing validation path is untouched.

## Assumed criteria

None. No criterion in context.md is marked `[ASSUMED]` or `[WITHDRAWN]`.

## Specification findings

None. The acceptance criteria are each independently testable and do not contradict one another.

## Summary

The production code change in `PetTypeRestControllerV1.java` correctly implements trimming for both create and rename, preserves internal spaces, preserves already-trimmed names, and preserves null-name behavior (AC1, AC2, AC3, AC5, AC6 all MET by direct code inspection). However, AC4 explicitly requires new tests covering AC1 through AC3 to be added to the existing controller test suite, and `PetTypeRestControllerV1Tests.java` contains no such tests — it is unchanged from before the story. The build gate passing (with one pre-existing, unrelated failure on the base branch) only confirms the old tests still pass; it does not substitute for the missing new tests the criterion calls for. Add tests asserting trimmed output for `"  hamster  "` → `"hamster"` (create, 201), `"  dog "` → `"dog"` (update, 204), and `"guinea pig"` → `"guinea pig"` (create, 201) to close AC4.
