# Validation — Trim spaces around a pet type name

**Source:** pet-type-name-trim-context-261116-065053.md
**Criteria checked:** 4  (met 3 · not met 1 · unverifiable 0)

**VERDICT: CHANGES_REQUESTED**

## Per-criterion verdicts

### AC1 — MET
`POST /api/pettypes` with `"name": "  hamster  "` returns 201 and the returned pet type has `"name": "hamster"`.
**Evidence:** `PetTypeRestControllerV1.addPetType` calls `petTypeFieldsDto.setName(trimName(petTypeFieldsDto.getName()))` before mapping the fields DTO to a `PetType` entity via `petTypeMapper.toPetType(petTypeFieldsDto)`, so the entity built from `"  hamster  "` already has `name == "hamster"`. The entity is then saved via `clinicService.savePetType(type)` and the response is built from that same (already-trimmed) entity: `petTypeMapper.toPetTypeDto(type)` with `HttpStatus.CREATED` (201). `trimName` is `name == null ? null : name.trim()`, which strips leading/trailing whitespace. No test in `PetTypeRestControllerV1Tests.java` exercises this exact input/output pair, but the production code path is unambiguous and self-contained — the trim happens before both persistence and response mapping, so no runtime condition could cause the returned name to retain surrounding spaces.

### AC2 — MET
`PUT /api/pettypes/{petTypeId}` with `"name": "  dog "` returns 204 and the pet type is saved with the name `"dog"`.
**Evidence:** `PetTypeRestControllerV1.updatePetType` sets `currentPetType.setName(trimName(petTypeDto.getName()))` on the looked-up entity before calling `this.clinicService.savePetType(currentPetType)`, so the persisted entity's name is `"dog"` (via the same `trimName`/`String.trim()` logic as AC1). The method returns `HttpStatus.NO_CONTENT` (204) on the success path (entity found). `testUpdatePetTypeSuccess` in `PetTypeRestControllerV1Tests.java` exercises this method shape (mocked `findPetTypeById`, `put`, asserts 204, then verifies the saved name via a follow-up `GET`), confirming the mechanism works for a non-trimmed name (`"dog I"`); no test uses a padded input, but the trim call sits unconditionally ahead of `savePetType` for every update, so the behaviour for `"  dog "` follows the same code path.

### AC3 — MET
`POST /api/pettypes` with `"name": "guinea pig"` returns 201 and the returned name is `"guinea pig"`.
**Evidence:** `trimName("guinea pig")` returns `"guinea pig"` unchanged — `String.trim()` only removes characters at the start and end of the string, so the internal space between "guinea" and "pig" is preserved. The same `addPetType` flow as AC1 (trim → map → save → map to DTO with 201) applies, so a name with no leading/trailing whitespace is returned exactly as sent.

### AC4 — NOT_MET
The existing pet type controller tests still pass, with new tests for AC1 to AC3.
**Gap:** `src/test/java/org/springframework/samples/petclinic/rest/controller/PetTypeRestControllerV1Tests.java` contains no new test methods covering the trimming behaviour. The only create/update tests present are `testCreatePetTypeSuccess` (uses the untouched `petTypes.get(0)`, name `"cat"`, no surrounding spaces), `testCreatePetTypeError` (null name, pre-existing 400 case), `testUpdatePetTypeSuccess` (name set to `"dog I"`, no surrounding spaces), and `testUpdatePetTypeError` (empty-string name, pre-existing 400 case). None of these assert on a padded input such as `"  hamster  "`, `"  dog "`, or confirm that `"guinea pig"` is returned unchanged with internal spacing intact. No regression test exists for any of AC1, AC2, or AC3, so this criterion's "with new tests for AC1 to AC3" clause is unsatisfied even though the underlying production code (per AC1–AC3 above) implements the behaviour correctly.
**Where:** `src/test/java/org/springframework/samples/petclinic/rest/controller/PetTypeRestControllerV1Tests.java` — no trimming-specific test methods present.

## Assumed criteria

None. All four criteria were taken verbatim from the task's acceptance-criteria list.

## Specification findings

None. The four criteria as given are each independently testable and do not conflict with one another or with the broader context.md acceptance criteria (AC-1 through AC-7), which describe the same behaviour in more detail.

## Summary

The production change in `PetTypeRestControllerV1` correctly trims leading/trailing whitespace from the submitted name on both add (`addPetType`) and rename (`updatePetType`), before persistence and before building the response DTO, satisfying AC1, AC2, and AC3 by direct code inspection. AC4 fails: the test file has not been updated with any new test methods exercising the padded-name scenarios from AC1–AC3 (or asserting the internal-space preservation from AC3); only the pre-existing null/empty-name error tests and the original non-padded success tests remain. A developer should add regression tests asserting the trimmed response/persisted value for `"  hamster  "`, `"  dog "`, and `"guinea pig"` before this story can be considered complete.
