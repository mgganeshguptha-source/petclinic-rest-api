## What Are We Trying to Achieve
Trim spaces at the beginning and end of pet type names when clinic administrators add or rename a pet type, so equivalent names are not stored with different surrounding spacing. Spaces within a name remain unchanged, and existing null-name behavior is preserved.

## Current Behaviour
The create operation maps the submitted fields to a pet type and saves it without trimming its name. The rename operation assigns the submitted name to the existing pet type and saves it without trimming. Existing controller tests expect a bad-request response for a null name on create and for an empty name on update.

## Expected Behaviour
For `POST /api/pettypes` and `PUT /api/pettypes/{petTypeId}`, remove spaces at the start and end of `name` before saving. Return the trimmed name in the create response; the update response remains 204. Preserve spaces within the name and preserve names already lacking surrounding spaces. Keep null-name behavior and validation responses unchanged. Do not change the API contract.

## Acceptance Criteria
- AC1: WHEN `POST /api/pettypes` receives `"name": "  hamster  "`, THE pet type endpoint SHALL return status 201 with `"name": "hamster"` in the created pet type.
- AC2: WHEN `PUT /api/pettypes/{petTypeId}` receives `"name": "  dog "`, THE pet type endpoint SHALL save the name `"dog"` and return status 204.
- AC3: WHEN `POST /api/pettypes` receives `"name": "guinea pig"`, THE pet type endpoint SHALL return status 201 with the name `"guinea pig"`.
- AC4: WHEN the existing pet type controller test suite is run with tests for AC1 through AC3, THE test suite SHALL pass.
- AC5: WHEN either write operation receives a name with no spaces at its start or end, THE pet type endpoint SHALL save the name exactly as received.
- AC6: WHEN either write operation receives a null name, THE pet type endpoint SHALL retain the existing null-name behavior without adding validation or a new error response.

## Edge Cases
- Null or missing `name`: preserve current behavior; the existing create test expects a bad-request response for a null name.
- Empty `name`: preserve current validation behavior; the existing update test expects a bad-request response.
- A name containing internal spaces, such as `"guinea pig"`, keeps those spaces.
- A name with no surrounding spaces is saved exactly as sent.
- Names with surrounding spaces are trimmed on both create and rename.
- `name` is a free-form string, not an enum; no unknown-enum behavior applies.
- The OpenAPI schema currently describes `name` as a required string with length 1–80; do not change that contract.

## Constraints
- Keep the change limited to the pet type create and update behavior.
- Preserve the existing request and response shapes, HTTP statuses, and null/validation behavior.
- Do not change the OpenAPI contract.
- Follow the existing Spring Boot patterns: constructor injection, thin controllers, DTOs and the existing mapper, and service-layer persistence.
- Add the requested cases to the existing JUnit 5 / Spring Boot / MockMvc controller tests and keep existing tests passing.
- Create and rename operations require the existing `VET_ADMIN` role.
- Keep error responses consistent with the existing centralized exception handling.

## Out of Scope
- Changes to the OpenAPI contract or generated API models.
- Changes to pet type model, mapper contracts, repository behavior, or database schemas.
- Changes to any other controller or endpoint.
- Changes to validation rules or null-name behavior.
- Changes to the Angular frontend.

## Clarifications Needed
None identified. The affected operations, example inputs and outputs, internal-space behavior, unchanged null behavior, and API-contract scope are specified.

## Story Quality Score
| Dimension | Score | Basis |
|---|---|---|
| Clarity | 20/20 | The target operations and name-trimming behavior are explicit. |
| Testability | 19/20 | Create and rename outcomes are concrete; null behavior is defined as unchanged rather than restating every existing response. |
| Traceability | 20/20 | Criteria trace to the story, its acceptance criteria, or existing behavior. |
| Atomicity | 20/20 | Each criterion states one testable outcome. |
| Completeness | 19/20 | The main inputs, statuses, and preservation rules are covered. |
| Edge coverage | 19/20 | Null, empty, internal spaces, and already-trimmed names are addressed. |
| **Total** | **116/120** | |

## Design trigger
**DESIGN REQUIRED: NO**
Follows the existing pet type create/update flow through the controller, mapper, and clinic service; the API contract and persistence interfaces remain unchanged.

## Feasibility
**VERDICT: GO**
Basis: Both write operations and the existing save service are present, and the requested behavior is confined to those operations and their controller tests.
Data flow: Request DTO → pet type controller and mapper (this repository) → clinic service and existing persistence implementation (this repository) → HTTP response (this repository).
