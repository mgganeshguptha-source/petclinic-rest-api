# Copilot Instructions — spring-petclinic-rest

## Repository Overview

- Purpose: REST API for the Spring PetClinic sample domain (owners, pets, pet types, visits, vets, specialties, users). Primary consumers: [TODO — MUST COMPLETE]: Name the client applications (web, mobile, third-party) that call this API and what they depend on. Needed so Copilot understands which endpoints and response shapes must stay stable.
- Tech Stack: Spring Boot 4.1.1 (parent `spring-boot-starter-parent`), project version 4.0.2, Maven build. Java version: [TODO — MUST COMPLETE]: Confirm the exact Java release (pom enforces `java.version` via maven-enforcer; `maven.compiler.release` is set through a property). Key dependencies: Spring Web MVC, Spring Data JPA, Spring JDBC (`spring-data-jdbc-core`), Spring Security, Spring Validation (Jakarta Bean Validation), Spring Cache, AspectJ, Actuator, MapStruct 1.6.3, springdoc-openapi 3.1.1, Jackson 3 (`tools.jackson`), jackson-databind-nullable, OpenAPI Generator Maven plugin. Datastores: H2 (default/dev), HSQLDB, MySQL, PostgreSQL.
- Architecture: Layered REST API — controller (`rest/controller/v1`, `rest/controller/v2`) → service (`service/ClinicService`, `service/UserService`) → repository (`repository/`) → JPA entities (`model/`). DTOs live in `rest/dto` (generated from the OpenAPI spec) and are converted to and from entities with MapStruct mappers in `mapper/`. API interfaces (`rest/api`) are generated from the OpenAPI spec by the OpenAPI Generator plugin; controllers implement them. Repositories have two interchangeable implementations: `repository/jpa` and `repository/jdbc`, plus Spring Data JPA interfaces in `repository/springdatajpa`. Do not modify generated sources under `target/generated-sources`; change the OpenAPI spec and regenerate.

## API Conventions

- Versioning: path-based via the controller package (`v1`, `v2`). Current request mappings use the `/api` base path (`@RequestMapping("/api")`) with per-resource paths such as `/api/owners/{id}`. New endpoints must follow the existing version the resource already uses; do not introduce a new versioning scheme.
- Error responses: RFC 9457 `ProblemDetail`, produced by `rest/advice/ExceptionControllerAdvice`. Shape: `type` (request URL), `title` (exception simple name), `status`, `detail` (fixed human-readable message), plus properties `timestamp` and `schemaValidationErrors` (list of `{message, field, rejectedValue, defaultMessage}`, populated for bean-validation failures). Map new exceptions in this advice; do not return ad-hoc error bodies from controllers.
- Status codes in use: 200 OK for reads, 201 CREATED with a `Location` header for creates, 404 NOT_FOUND for missing resources (controllers return `new ResponseEntity<>(HttpStatus.NOT_FOUND)` when a lookup returns null or empty), 400 BAD_REQUEST for validation failures, 500 for unexpected errors. Note: `DataIntegrityViolationException` is currently mapped to 404 in the advice; do not copy this mapping into new code without confirming intent.
- Input validation: Jakarta Bean Validation annotations on the OpenAPI-generated DTOs and custom validators (e.g., `rest/validation/PetAgeValidation`). Validate request bodies with `@Valid`-style constraints, not manual checks in controllers.
- Pagination: service exposes `Pageable`-based methods (e.g., `ClinicService.findPets(Pageable)`). Pagination query parameter names and response envelope: [TODO — MUST COMPLETE]: Confirm the list-endpoint pagination contract (parameter names, default page size, response shape) for consistency across resources.
- Persistence-level behaviour of creates/updates: controllers call the service, then map the entity to a DTO for the response.

## Security Guidelines

Standard DOs and DON'Ts:
- DO use `@PreAuthorize` with the `@roles` bean (e.g., `hasRole(@roles.OWNER_ADMIN)`) on every controller method that reads or writes data. Follow the pattern in `OwnerRestControllerV1`.
- DO NOT hardcode credentials, API keys, or connection strings; use externalised configuration (`application*.properties`, environment variables).
- DO NOT build SQL by string concatenation. Use JPA/Spring Data query methods or JDBC parameter binding (see `repository/jdbc`).
- DO NOT log passwords, tokens, or personal data about owners or users.
- DO validate all inbound request bodies with Jakarta Validation.
- DO hash passwords with BCrypt (`BCryptPasswordEncoder` is the configured `PasswordEncoder`). Never store or compare plaintext passwords.

Project-specific facts:
- Authentication: HTTP Basic, configured in `security/BasicAuthenticationConfig`, active only when `petclinic.security.enable=true`. Credentials and roles are loaded from `users` and `roles` tables via JDBC (`usersByUsernameQuery`, `authoritiesByUsernameQuery`). When disabled, `security/DisableSecurityConfig` applies. Do not add endpoints that bypass the security filter chain.
- Authorization: role names are defined in `security/Roles`. Use these constants; do not hardcode role strings.
- CSRF is disabled in the filter chain because the API is stateless HTTP Basic. Do not re-enable it for API routes without discussion.
- CORS: controllers use `@CrossOrigin(exposedHeaders = "errors, content-type")` with no explicit origin list. [TODO — MUST COMPLETE]: Specify the allowed origins for each deployment environment and whether a wildcard origin is acceptable. Do not add `*` origins for authenticated endpoints.
- Transport security (TLS) and any token-based auth (JWT/OAuth2) plans: [TODO — MUST COMPLETE]: Describe the production transport and identity strategy. HTTP Basic over plain HTTP is not acceptable in production.
- Actuator endpoints: [TODO — MUST COMPLETE]: List which actuator endpoints are exposed and whether they require authentication.

## Design Patterns

Patterns the codebase actually uses:
- Layered architecture with a service facade (`ClinicService` / `ClinicServiceImpl`) and `@Transactional` boundaries on service methods (`readOnly = true` for reads).
- Repository pattern with interchangeable JPA and JDBC implementations behind the same interfaces.
- DTO + mapper: MapStruct mappers (`OwnerMapper`, `PetMapper`, `VisitMapper`, etc.) convert between entities and generated DTOs. Do not expose JPA entities directly as request or response bodies.
- OpenAPI-first contract: API interfaces and DTOs are generated; controllers implement the generated interfaces.
- Constructor injection for all Spring beans. Do not add field injection.
- Cross-cutting concerns via AOP: `util/CallMonitoringAspect` for call monitoring; caching via `spring-boot-starter-cache`.
- Global exception handling with `@ControllerAdvice`.

## Unique Patterns For This Project

[TODO — MUST COMPLETE]: Project-specific rules an AI would not infer from code, for example:
- Date and timezone handling (e.g., UTC storage, conversion at the edge; the pet birth-date rule enforced by `PetAgeValidator`).
- Internationalisation (message source, whether user-facing messages must be translated).
- Idempotency requirements for create and update endpoints.
- Audit logging requirements (who changed what, retention, where audit events go).
- Any difference between the JPA and JDBC repository implementations that callers must know about.
These cause the most defects when missed. Fill before relying on this file.

## What To Avoid

- Business logic in controllers. Controllers translate HTTP to service calls and back.
- JPA entities as request or response bodies. Use DTOs and MapStruct mappers.
- Editing generated code (`target/generated-sources`, `rest/api`, generated DTOs). Change the OpenAPI spec and regenerate.
- Field injection, and circular dependencies between services.
- Transactions on controllers or on private methods; keep `@Transactional` in the service layer.
- Returning HTTP 200 with an error body. Use `ProblemDetail` error responses via the advice.
- Adding a new repository implementation without matching the existing interface contract in both `jpa` and `jdbc` packages, unless the team decides otherwise (see Unique Patterns).
- Adding endpoints without `@PreAuthorize` and without a documented role.
- Fetching all rows (`findAll`) on list endpoints that could grow large without pagination.

## Documentation Requirements

- Public controller methods and service interfaces: Javadoc in the existing style (`@author` tags are used on classes; add a short description on public methods).
- OpenAPI: all endpoints are documented in the OpenAPI spec that drives generation and springdoc. Keep the spec in sync when changing a contract.
- Project documentation (README, environment variables, Spring profiles, how to run with H2/MySQL/PostgreSQL): [TODO — MUST COMPLETE]: Confirm what must be updated in the README and whether a changelog is required for API changes.
- Team expectations on code comments and PR descriptions for contract changes: [TODO — MUST COMPLETE]: Specify the expected level of detail.
