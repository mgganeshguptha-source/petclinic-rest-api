## Summary

Trims leading and trailing spaces from a pet type `name` when a pet type is added or renamed, so the pet type list does not show the same type twice with different spacing.

## Changes

- `PetTypeRestControllerV1`: the add (`POST /api/pettypes`) and update (`PUT /api/pettypes/{petTypeId}`) methods remove spaces at the start and end of `name` before the pet type is saved. The add response returns the trimmed name.
- `PetTypeRestControllerV1Tests`: new tests for the acceptance criteria below.

## Behaviour

- `"  hamster  "` is saved and returned as `"hamster"`.
- Spaces inside a name are kept (`"guinea pig"` stays `"guinea pig"`).
- A name with no surrounding spaces is saved exactly as sent.
- A `null` name is handled as it was before. No new validation or error responses are added.

## Contract

No API contract change. `src/main/resources/openapi.yml` is unchanged.

## Acceptance criteria

- AC1: `POST /api/pettypes` with `"name": "  hamster  "` returns 201 and the returned name is `"hamster"`.
- AC2: `PUT /api/pettypes/{petTypeId}` with `"name": "  dog "` returns 204 and the pet type is saved as `"dog"`.
- AC3: `POST /api/pettypes` with `"name": "guinea pig"` returns 201 and the returned name is `"guinea pig"`.
- AC4: Existing pet type controller tests still pass, with new tests for AC1 to AC3.

## Out of scope

`openapi.yml`, model and repository classes, database schemas, other controllers, and the Angular front end.

## Build status

The harness built this exact tree and ran the repository test command at its build gate. The build **passed**; the full output is in `.harness/validation-report.txt`.

Some tests were already failing on the base branch before this change. The harness ignored them at the build gate, and every other test passed. The build is therefore not fully green. The harness lists the pre-existing failures in the pull request.

<!-- harness:baseline-failing-tests -->
## Tests already failing before this change

These 1 test(s) failed on the base branch (commit `d15a37e0d2b8`) before the harness changed anything. The build gate ignored them; every other test passed. They cannot catch a fault in the code they cover until they are fixed.

- `org.springframework.samples.petclinic.model.ValidatorTests#deliberatelyFailingForHarnessBaselineTest`
