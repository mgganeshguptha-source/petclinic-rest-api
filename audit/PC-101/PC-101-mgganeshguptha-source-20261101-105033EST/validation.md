# Validation: Add a status to each visit in the Visit API

Basis: `.github/story-context-files/visit-status-context-261101-155129.md`. Its AC numbering (AC-1 to AC-11) is used here. The context file notes its ACs were reconstructed (quality 6/10), so the original story text should be checked. Validation is read-only: no shell, no test run. `./mvnw test` was not executed.

### AC-1 — MET
`openapi.yml` defines `VisitStatus` (SCHEDULED, COMPLETED, CANCELLED) once under components/schemas (~L2286).

### AC-2 — MET
`VisitFields.status` (openapi.yml ~L2282–2283) references `#/components/schemas/VisitStatus`. `Visit` is an allOf of `VisitFields`.

### AC-3 — MET
`model/Visit.java` initialises `status = SCHEDULED`. `setStatus(null)` falls back to SCHEDULED. No test asserts this.

### AC-4 — MET
`status` column is NOT NULL with default 'SCHEDULED' in all four profiles: h2 L59 and hsqldb L64 (DEFAULT before NOT NULL), mysql L54 and postgres L51. Seed inserts in hsqldb/h2 use explicit column lists, so the default applies.

### AC-5 — MET
Status is mapped by name through `VisitMapper` to the DTO. The JDBC row mapper (`JdbcVisitRowMapper`) reads `visit_status` on the GET paths. Verified by reading, not by a test.

### AC-6 — MET
`VisitRestControllerV1.updateVisit` sets status when `visitDto.getStatus() != null`, and the value is persisted through `JdbcVisitRepositoryImpl` update params.

### AC-7 — UNVERIFIABLE
`ExceptionControllerAdvice.handleHttpMessageNotReadableException` (L108–150) returns 400 ProblemDetail when the exception chain message contains `VisitStatusDto`'s FQN. This depends on the exact Jackson 3 enum-deserialisation message, which cannot be confirmed without running the app. No test exercises it.

### AC-8 — MET
`JdbcOwnerRepositoryImpl` SQL (L140) selects `visits.status as visit_status`, and `JdbcPetVisitExtractor` uses `JdbcVisitRowMapper`, which reads it. Owner nested visits (openapi.yml ~L2202) reference `Visit`, which includes status.

### AC-9 — MET
`VisitRestControllerV1` carries `@PreAuthorize("hasRole(@roles.OWNER_ADMIN)")` on list, get, add, update, and delete (per the context file's current-state record). No new endpoint was added. Not re-read in this phase.

### AC-10 — MET
`VisitRestControllerV1.updateVisit` only applies status when non-null, so an absent or null value leaves the stored value unchanged.

### AC-11 — MET
Null or absent status never triggers 400. A null create falls back to SCHEDULED via `Visit.setStatus`, and a null update skips the assignment. The 400 path is reached only for an unreadable body (see AC-7).

---

**Summary:** 10 MET, 0 NOT_MET, 1 UNVERIFIABLE (AC-7). No test in `src/test/java` references status, so the context's test plan is not evidenced.

**VERDICT: INCONCLUSIVE**
