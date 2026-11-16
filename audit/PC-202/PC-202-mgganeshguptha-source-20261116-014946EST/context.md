# context.md

## What Are We Trying to Achieve
Normalize pet type names when clinic administrators add or rename a type by removing spaces at the beginning and end before persistence. This avoids separate entries that differ only by surrounding spaces while preserving spaces within a name and leaving the API contract unchanged.

## Current Behaviour
The add operation maps the submitted name to a pet type and saves it without trimming. The rename operation assigns the submitted name directly to the existing pet type and saves it without trimming. Existing controller tests observe a 400 response for a null name on create and for an empty name on update.

## Expected Behaviour
On add and rename, remove spaces at the start and end of the name before saving. The create response contains the trimmed name and keeps its existing 201 status; rename keeps its existing 204 status. Spaces within a name remain unchanged, and a name without surrounding spaces is saved unchanged. Null names keep their current method-specific behavior. Do not add validation or error responses, and do not change the OpenAPI contract.

## Acceptance Criteria
- AC-1: WHEN `POST /api/pettypes` receives `"name": "  hamster  "`, THE pet type endpoint SHALL return status 201 with the returned name `"hamster"`.
- AC-2: WHEN `PUT /api/pettypes/{petTypeId}` receives `"name": "  dog "`, THE pet type endpoint SHALL save the name `"dog"` and return status 204.
- AC-3: WHEN `POST /api/pettypes` receives `"name": "guinea pig"`, THE pet type endpoint SHALL return status 201 with the name `"guinea pig"`.
- AC-4: THE pet type controller test suite SHALL include regression tests for AC-1 through AC-3.
- AC-5: WHEN the existing pet type controller tests run, THE pet type controller test suite SHALL continue to pass.
- AC-6: WHEN a submitted name has no spaces at its start or end, THE pet type endpoint SHALL save that name unchanged.
- AC-7: IF a submitted name is null, THEN THE pet type endpoint SHALL retain the existing method-specific behavior without adding validation or an error response.

## Edge Cases
- Null `name`: preserve the current behavior for each operation; the existing create test expects 400 for null.
- Empty `name`: preserve current validation behavior; the existing update test expects 400 for an empty string.
- Internal spaces: preserve them, as in `"guinea pig"`.
- A non-empty name made only of spaces becomes empty after trimming; do not introduce new validation or an error response.
- A name without leading or trailing spaces is unchanged.

## Constraints
- Keep the OpenAPI contract unchanged.
- Limit the change to add/rename behavior and the related pet type controller tests; do not change models, repositories, database schemas, other controllers, or the Angular frontend.
- Preserve existing validation and error behavior; add no new validation or error responses.
- Keep the existing authorization on these write operations (`VET_ADMIN`).
- Follow the repository's constructor-injection and controller-to-service patterns.
- Use the existing JUnit 5 / Spring test setup and established controller test conventions.
- The project uses Java and Spring Boot 4.1.1 with Maven; the exact Java release is inherited and not explicitly declared in this project's POM.

## Out of Scope
- Changes to `openapi.yml` or any generated API contract.
- Changes to pet type model classes, repositories, or database schemas.
- Changes to controllers other than the pet type add and rename operations.
- Changes to the Angular frontend or other client applications.
- New validation rules or new error responses.
- Removing or normalizing spaces within a name.

## Story Quality Score
| Dimension | Score | Basis |
|---|---|---|
| Clarity | 20/20 | Inputs, normalization rule, preserved behavior, and exclusions are explicit. |
| Testability | 19/20 | The examples and expected statuses are testable; null behavior is specified by reference to existing behavior. |
| Traceability | 20/20 | Criteria are based on the story and its stated scope; no assumed criteria were added. |
| Atomicity | 18/20 | AC-2 combines persisted value and HTTP status for one update request; AC-4 and AC-5 separate regression coverage from preserving existing tests. |
| Completeness | 20/20 | Add, rename, response, persistence, unchanged names, validation boundaries, and contract scope are covered. |
| Edge coverage | 19/20 | Null, empty, all-space, internal-space, and unchanged-name cases are covered; unmentioned non-space whitespace characters are not prescribed. |
| **Total** | **116/120** | |

## Design trigger
**DESIGN REQUIRED: NO**
The add and rename operations already follow the controller-to-service flow used for pet type persistence, and this story explicitly confines the behavior to those operations without changing the API contract or introducing a new pattern.

## Feasibility
**VERDICT: GO**
Basis: The existing controller, mapper, service, repository path, and controller test setup provide the components needed for the requested behavior.
Data flow: Request → controller and mapper → service → repository; the create response is mapped from the saved entity.
Note: An out-of-scope pet type model currently contains a deliberate compile error (an `int` method returns a `String`). This pre-existing issue blocks build/test verification until resolved separately; no source files were changed for this context.
