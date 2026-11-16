# Trim spaces around a pet type name

## Summary

Leading and trailing spaces are removed from a pet type `name` when it is added or renamed. This stops the pet type list from showing the same type twice with different spacing.

## Behaviour

| Endpoint | Change |
|---|---|
| `POST /api/pettypes` | `name` is trimmed before save. The response returns the trimmed name. |
| `PUT /api/pettypes/{petTypeId}` | `name` is trimmed before save. |

- Spaces inside a name are kept (`"guinea pig"` stays `"guinea pig"`).
- A name with no surrounding spaces is saved exactly as sent.
- A `null` name is passed through unchanged, as before. No new validation or error responses are added.
- The OpenAPI contract (`src/main/resources/openapi.yml`) does not change.

## Implementation

File: `src/main/java/org/springframework/samples/petclinic/rest/controller/v1/PetTypeRestControllerV1.java`

- A private helper `trimName(String)` returns `null` for `null` and `name.trim()` otherwise.
- `addPetType` applies it to `petTypeFieldsDto` before mapping to the entity.
- `updatePetType` applies it to the loaded entity's name before `savePetType`.

No model, repository, schema, or other controller changes.

## Acceptance criteria

| # | Criterion | Test status |
|---|---|---|
| AC1 | `POST /api/pettypes` with `"name": "  hamster  "` returns 201 and `"name": "hamster"`. | Not yet covered by a test. |
| AC2 | `PUT /api/pettypes/{petTypeId}` with `"name": "  dog "` returns 204 and saves `"dog"`. | Not yet covered by a test. |
| AC3 | `POST /api/pettypes` with `"name": "guinea pig"` returns 201 and `"guinea pig"`. | Not yet covered by a test. |
| AC4 | Existing pet type controller tests pass, plus new tests for AC1 to AC3. | Open. New tests are still needed in `PetTypeRestControllerV1Tests.java`. |

## Open items

- Add tests for AC1 to AC3 in `src/test/java/org/springframework/samples/petclinic/rest/controller/PetTypeRestControllerV1Tests.java`.
- The `PUT` test should assert that the saved entity has the trimmed name, for example with a mock `ClinicService` argument captor or by re-reading the entity.

## How to verify

This change was not compiled or tested as part of this documentation. Before approving:

1. Build the project with Maven (`./mvnw clean verify` or the equivalent in your environment).
2. Run `PetTypeRestControllerV1Tests` and confirm the existing tests pass.
3. Confirm that new tests for AC1 to AC3 exist and pass.
4. Manually check `POST /api/pettypes` with `"  hamster  "` returns `"hamster"`.
