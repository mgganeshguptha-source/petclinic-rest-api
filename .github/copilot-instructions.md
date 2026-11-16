# Copilot Instructions — spring-petclinic-rest (Spring Boot backend)

## 1. Repository Overview

- **Purpose:** [TODO — MUST COMPLETE]: One-paragraph description of what this service does and its primary consumers. Needed so Copilot understands domain context.
- **Tech Stack:** Spring Boot 4.1.1 (`spring-boot-starter-parent`), Maven 3.9.9 build, Spring Web MVC, Spring Data JPA, Spring JDBC (with `spring-data-jdbc-core` 1.2.1.RELEASE), Spring Security (HTTP Basic, BCrypt, method security via `@PreAuthorize`), Jakarta Validation, Spring Cache, Actuator, AspectJ, MapStruct 1.6.3 (with `mapstruct-processor`), springdoc-openapi 3.1.1 (Swagger UI), jackson-databind-nullable 0.2.11, JUnit 5 + Mockito + Spring Security Test.
  - Datastores: H2 (default/dev), HSQLDB, MySQL, PostgreSQL. Selected via Spring profiles `h2`, `hsqldb`, `mysql`, `postgres` (`src/main/resources/application-*.properties`).
  - Java version: not pinned in `pom.xml`; it is inherited from the Spring Boot parent. [TODO — MUST COMPLETE]: Confirm the target JDK and state it here.
- **Architecture:** Layered, OpenAPI-first REST API.
  - `rest/controller/v1`, `rest/controller/v2` — REST controllers (`@RestController`) implementing interfaces generated from `src/main/resources/openapi.yml` (package `rest.api`, DTOs in `rest.dto`).
  - `mapper` — MapStruct mappers between model entities and DTOs (`defaultComponentModel = spring`).
  - `service` — `ClinicService` / `UserService` and their `*Impl` classes. Transaction boundaries live here.
  - `repository` — repository interfaces with three implementations: `jpa`, `springdatajpa`, and `jdbc`.
  - `model` — JPA entities (`Owner`, `Pet`, `Visit`, `Vet`, `Specialty`, `PetType`, `User`, `Role`).
  - `security` — `BasicAuthenticationConfig` (enabled only when `petclinic.security.enable=true`) and `DisableSecurityConfig`.
  - `rest/advice` — `ExceptionControllerAdvice` (global error handling).
  - `util` — `CallMonitoringAspect`, `EntityUtils`.

## 2. API Conventions

- **Versioning:** Two generations of controllers exist, `v1` and `v2`, in separate packages. [TODO — MUST COMPLETE]: Confirm the URL prefix for each version and the deprecation policy for v1. Do not add endpoints to v1 without confirming this.
- **Spec-first:** The API contract is `src/main/resources/openapi.yml`. Change the spec first, then regenerate. Do not hand-write DTOs or `*Api` interfaces that the generator already produces.
- **Error response format:** All errors use RFC 9457 `ProblemDetail` (built in `ExceptionControllerAdvice`). Properties present:
  - `type`, `title` (exception simple name), `status`, `detail`
  - `timestamp` (Instant)
  - `schemaValidationErrors` (list of `{message, field, rejectedValue, defaultMessage}`), populated for validation failures
  - Do not return stack traces or internal messages to clients. Log them server-side only.
- **Validation:** Use Jakarta Validation annotations on DTOs and `@Valid` on request bodies. Bean validation failures return 400 via `MethodArgumentNotValidException`. Custom validators exist, for example `PetAgeValidation`.
- **Status codes, as observed:**
  - Create returns 201 Created with a `Location` header (`/api/owners/{id}` pattern).
  - Get, update, and delete of a missing resource return 404.
  - List endpoints return 404 when the result is empty. This is the current behaviour; do not copy it into new endpoints without confirming. [TODO — MUST COMPLETE]: Confirm whether empty lists should return 200 with `[]` for new endpoints.
  - `DataIntegrityViolationException` is mapped to 404 with a generic message. [TODO — MUST COMPLETE]: Confirm this mapping is intended. Constraint violations are usually 409 or 422.
- **Pagination:** `ClinicService.findPets(Pageable)` exists, so Spring Data `Pageable` is used in the service layer. [TODO — MUST COMPLETE]: Confirm the public query parameter names and default page size.
- **CORS:** Controllers use `@CrossOrigin(exposedHeaders = "errors, content-type")`. [TODO — MUST COMPLETE]: Confirm the allowed origins for production. Do not add wildcard origins.

## 3. Security Guidelines

- **Authentication:** HTTP Basic, enabled only when `petclinic.security.enable=true`. Users and roles are read from the database through JDBC (`users` and `roles` tables) with BCrypt password encoding.
- **Authorization:** Every controller method is guarded with `@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")` or the equivalent role constant from the `Roles` bean. Every new endpoint must have an explicit `@PreAuthorize`. Do not rely on the default.
- **DOs:**
  - Use constructor injection.
  - Validate all inbound data with Jakarta Validation.
  - Use BCrypt (or the configured `PasswordEncoder`) for passwords. Never store or compare plaintext.
  - Use parameterised JPA/JDBC queries. Never build SQL by string concatenation.
  - Return DTOs, never JPA entities, from controllers.
- **DON'Ts:**
  - Never hardcode secrets, credentials, or connection strings. Use externalised configuration.
  - Never log passwords, tokens, or request/response bodies that contain owner, pet, or visit data.
  - Never expose the full entity graph (for example `Owner` with all `Pet`s and `Visit`s) when the endpoint needs a subset.
- **CSRF:** The current `BasicAuthenticationConfig` disables CSRF. [TODO — MUST COMPLETE]: Confirm this is intentional for a stateless HTTP Basic API. Do not extend this to any cookie-based or browser-session flow without review.
- **Data sensitivity:** Owner names, addresses, phone numbers, and visit records are personal and clinical data. Apply the repository's `hipaa-guardrails` rules to them. [TODO — MUST COMPLETE]: Confirm the compliance scope (HIPAA or otherwise) and the data classification for each entity.
- **Other security items:** [TODO — MUST COMPLETE]: Project-specific security policy not visible in code, such as token strategy, password policy, account lockout, and TLS termination point.

## 4. Design Patterns

Patterns the codebase actually uses:

- **Layered architecture:** Controller → Service → Repository → Entity. Controllers call services only, never repositories.
- **Facade service:** `ClinicServiceImpl` is the facade for all clinic controllers. It owns `@Transactional` and `@Transactional(readOnly = true)` boundaries.
- **Repository abstraction with multiple implementations:** Each repository interface has `jpa`, `springdatajpa`, and `jdbc` implementations. Select by profile or configuration. Add new methods to the interface and to every implementation that is active.
- **DTO + MapStruct mapper:** Mappers in `mapper/` convert between entities and generated DTOs. Do not map by hand in controllers.
- **OpenAPI-first code generation:** `openapi-generator-maven-plugin` (generator `spring`, documentation provider `springdoc`) generates API interfaces and DTOs during the build.
- **Aspect-oriented monitoring:** `CallMonitoringAspect` (AspectJ) applies cross-cutting monitoring. [TODO — MUST COMPLETE]: Confirm its pointcut and what it records.
- **Method-level security:** `@PreAuthorize` with a `Roles` bean.
- **Global exception handling:** `@ControllerAdvice` in `rest/advice`.

## 5. Unique Patterns For This Project

- [TODO — MUST COMPLETE]: Project-specific rules an AI would not infer from code, such as date and timezone handling (for example, UTC storage and conversion at the edge), i18n strategy, idempotency requirements, and audit logging. These cause the most defects when missed.
- [TODO — MUST COMPLETE]: Explain how the three repository implementations relate. Which one is the production path, and which are kept for teaching or testing?
- [TODO — MUST COMPLETE]: Explain the `Owner`/`Pet`/`Visit` ownership rules and cascade behaviour, including what happens on delete.

## 6. What To Avoid

- Business logic in controllers. Put it in the service layer.
- JPA entities as request or response objects.
- Hand-editing generated sources under `target/generated-sources/openapi`. Change `openapi.yml` and rebuild.
- Mixing transaction annotations. The service layer uses `org.springframework.transaction.annotation.Transactional`, while `OwnerRestControllerV1` imports `jakarta.transaction.Transactional`. Use the Spring annotation in new code. Do not add transactions to controllers.
- Adding a new repository method to one implementation and not the others when more than one is active.
- Circular service dependencies.
- Returning 404 for every database error. Map each exception to the status it actually represents.
- Disabling tests or removing `@PreAuthorize` to make a test pass.

## 7. Documentation Requirements

- **API documentation:** `openapi.yml` is the source of truth. Keep it in sync with controller behaviour. Swagger UI is generated by springdoc (`config/SwaggerConfig.java`).
- **Code documentation:** Public classes and public service/controller methods carry Javadoc with `@author` on classes. Add Javadoc to new public APIs. Only comment code where the reason is not obvious.
- **Configuration:** Document new properties in `application.properties` and the matching profile files.
- [TODO — MUST COMPLETE]: Team expectations for README updates, changelog entries, and ADRs.
