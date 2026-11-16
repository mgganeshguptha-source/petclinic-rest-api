# Copilot Instructions — spring-petclinic-rest

## Repository Overview

- Purpose: REST API for the Spring PetClinic sample domain (owners, pets, pet types, visits, vets, specialties, users). Primary consumers: [TODO — MUST COMPLETE]: Name the client applications (web, mobile, integration partners) that call this API. Needed so Copilot understands who depends on response shapes and versions.
- Tech Stack (from `pom.xml`): Spring Boot 4.1.1 (parent `spring-boot-starter-parent`), Maven, Spring Web MVC, Spring Data JPA, Spring JDBC, Spring Security (HTTP Basic, BCrypt), Jakarta Validation, Spring Cache, Spring AOP (AspectJ), Actuator, MapStruct 1.6.3, springdoc-openapi 3.1.1, openapi-generator-maven-plugin, JUnit 5 + Mockito + Spring Security Test.
- Datastores: H2, HSQLDB (tests/dev), MySQL, PostgreSQL. Java version: [TODO — MUST COMPLETE]: Confirm the required JDK. The pom enforces `[${java.version},)` but the version is not set explicitly in the POM.
- Architecture: Layered REST API. Controller (`rest/controller/v1`, `rest/controller/v2`) → `ClinicService` / `UserService` (`service`) → repository interfaces (`repository`) → implementations per persistence profile (`repository/jpa`, `repository/jdbc`, `repository/springdatajpa`) → JPA entities (`model`). MapStruct mappers (`mapper`) convert entities to DTOs (`rest/dto`). API interfaces are generated from an OpenAPI spec by openapi-generator (`rest/api`).

## API Conventions

- Versioning: two controller generations, `v1` and `v2`, both mounted under `/api`. Owner list paging is available in v2 (`OwnerRestControllerV2`); v1 returns full lists. New endpoints should follow the version that owns their resource and must not change v1 response shapes.
- Pagination (v2 owners): zero-based `page` (default 0), `size` (default 20), sorted by `id`. Response is `OwnerPageDto` with `content`, `page`, `size`, `totalElements`, `totalPages`.
- Status codes (observed): `200` on reads, `201 Created` with `Location` header on create, `404 Not Found` when a resource or a list is empty, `400 Bad Request` on validation failure.
- Error response format: RFC 9457 `ProblemDetail` built in `ExceptionControllerAdvice`. Fields: `type` (request URL), `title` (exception simple name), `status`, `detail` (fixed safe message), `timestamp`, and `schemaValidationErrors` (list of `ValidationMessageDto` with `field`, `rejectedValue`, `defaultMessage`). Do not expose stack traces or raw exception messages to clients.
- Input validation: Jakarta Bean Validation on DTOs and `@Valid` request bodies. Custom rules live in `rest/validation` (e.g. `PetAgeValidation`).
- Request and response bodies are DTOs (`*Dto`, `*FieldsDto`, `*PageDto`). Never return JPA entities from controllers.
- CORS: controllers use `@CrossOrigin(exposedHeaders = "errors, content-type")`. Do not widen origins without a stated requirement: [TODO — MUST COMPLETE]: Name the allowed origins for production.

## Security Guidelines

- Do: annotate every controller method with `@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")` (or the equivalent role from the `Roles` bean). Use `@roles.*` constants, not hard-coded role strings.
- Do: keep credentials in configuration or a secret store. Passwords are stored with `BCryptPasswordEncoder`. Never store or compare plaintext passwords.
- Do: keep authentication on by default. Security is enabled by `petclinic.security.enable=true` (`BasicAuthenticationConfig`). Setting it to `false` activates `DisableSecurityConfig`, which removes authentication. Do not disable it in shared or production profiles.
- Do: use parameterized queries (JPA named parameters, JDBC `?` placeholders). Never build SQL by string concatenation.
- Do: validate every incoming DTO with Jakarta Validation before it reaches the service.
- Don't: log passwords, tokens, or personal data (owner names, addresses, telephone numbers). Log identifiers only.
- Don't: return stack traces, SQL text, or internal class names in responses. The `title` field carries the exception simple name and is the only exception detail returned.
- Don't: add `permitAll()`, disable CSRF for session-based flows, or add wildcard CORS without a documented reason.
- Project-specific: [TODO — MUST COMPLETE]: Authentication source and role model in production (JDBC `users`/`roles` tables are the current default). Also name token or session strategy if one is planned.

## Design Patterns

Patterns the codebase uses:

- Repository pattern with interface + implementation per persistence technology. Active implementation is chosen by Spring profile (`jpa`, `jdbc`, `spring-data-jpa`). New repository methods must be added to the interface and implemented in every active profile.
- Service facade: `ClinicService` (`ClinicServiceImpl`) groups repository calls for controllers. Keep transaction boundaries here with `@Transactional` (`readOnly = true` for reads).
- DTO + MapStruct mapping: mappers are Spring-managed interfaces (`@Mapper(uses = ...)`). Prefer adding a mapping method to the existing mapper over hand-written conversion code.
- OpenAPI-first controllers: controllers implement generated `*Api` interfaces. Change the OpenAPI spec first, regenerate, then implement.
- Constructor injection for all Spring beans. Do not use field injection in new code.
- Global exception handling with `@ControllerAdvice` (`ExceptionControllerAdvice`).
- Method-level security with `@PreAuthorize`.
- Cross-cutting monitoring via AspectJ (`util/CallMonitoringAspect`).

## Unique Patterns For This Project

- Persistence profiles: the same repository contract is implemented three ways (`jpa`, `jdbc`, `spring-data-jpa`). Tests run against all three (`ClinicService*Tests`, `UserService*Tests`). Any behaviour change must hold on every profile. This is the most common source of defects.
- Date and time: [TODO — MUST COMPLETE]: Time-zone policy for stored dates (e.g. `LocalDate` for birth dates, UTC for `Instant` timestamps) and the date format on the wire. Observed `LocalDate` in tests; the policy is not documented.
- Internationalisation: [TODO — MUST COMPLETE]: Whether error messages and validation text must be translated, and through what mechanism. Current messages are hard-coded English constants in `ExceptionControllerAdvice`.
- Idempotency: [TODO — MUST COMPLETE]: Whether POST/PUT endpoints must be idempotent (e.g. retried creates). No idempotency keys are implemented.
- Audit: [TODO — MUST COMPLETE]: Whether create, update, and delete operations on owners, pets, visits, and users must be audited, and where audit records go. No audit trail is implemented.
- Caching: `spring-boot-starter-cache` is present. [TODO — MUST COMPLETE]: Which caches exist, their eviction rules, and whether they must be invalidated on writes.

## What To Avoid

- Business logic in controllers. Controllers map, call the service, and set the HTTP status.
- JPA entities as request or response bodies. Use DTOs and MapStruct.
- Hand-written entity-to-DTO copying when a MapStruct mapping can express it.
- Field injection (`@Autowired` on fields) in new code. Use constructor injection.
- Transactions on controllers. Put `@Transactional` on service methods. Note that `OwnerRestControllerV1` currently uses `jakarta.transaction.Transactional`; prefer `org.springframework.transaction.annotation.Transactional` in new code.
- Calling repositories directly from controllers. Go through `ClinicService` or `UserService`.
- Editing generated sources under `target/generated-sources/openapi`. Change the spec and regenerate.
- Circular service dependencies.
- Adding a repository method to one persistence profile only.
- Changing v1 response shapes. Add v2 endpoints instead.

## Documentation Requirements

- Public service and controller methods carry Javadoc describing purpose, parameters, and return values. Existing `ExceptionControllerAdvice` methods show the expected style.
- API documentation is generated by springdoc-openapi. Keep OpenAPI annotations and spec in sync with behaviour. Spec location: [TODO — MUST COMPLETE]: Confirm the path of the source OpenAPI spec used by openapi-generator.
- Update the README when adding a profile, environment variable, or run mode. [TODO — MUST COMPLETE]: Confirm the README sections the team expects to keep current (e.g. profile list, Docker/Jib usage).
- Document every new environment property (`petclinic.security.enable`, datasource settings) where it is introduced.
