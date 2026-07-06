# Test Optimization Notes

Current goal: reduce feedback-loop time for `jimmer-sql` and
`jimmer-sql-kotlin` tests without changing test semantics.

## Implemented First Step

- Default H2 tests now use a shared in-memory database per test JVM:
  - Java: `jdbc:h2:mem:jimmer_test_db;...;DB_CLOSE_DELAY=-1`
  - Kotlin: `jdbc:h2:mem:jimmer_kt_test_db;...;DB_CLOSE_DELAY=-1`
- `database.sql` is initialized once per JVM instead of once per test class.
- Existing mutation helpers still use transactions and rollback, so ordinary
  mutation tests should not leak state into later tests.
- Raw H2 `jdbc(...)` test access now rolls back by default. Tests that only
  inspect fixture data should not mutate the shared database permanently.
- Tests that rely on generated ids should not assert the exact next sequence
  value unless they explicitly seed that generator in the test. Shared database
  reuse makes fixed auto-id expectations brittle and order-dependent.

## Current Guardrails

- Do not maintain hard-coded table or sequence reset lists in test utilities;
  they are easy to drift from `database.sql`.
- Prefer fixing tests so they do not depend on global generated-id state. If a
  test needs exact generated ids as part of the SQL contract, seed them locally
  with the existing helper and keep that setup visible in the test.

## Next Candidates

1. Add managed Testcontainers support for PostgreSQL/MySQL native tests while
   keeping local `localhost` mode available for explicit debugging.
2. Split generated test models from ordinary runtime test code so editing a test
   method does not force model annotation processing/KSP.
3. Reduce duplicated Java/Kotlin runtime test coverage; keep Kotlin tests focused
   on KSP, DTO/input, DSL, and Kotlin-specific typing/nullability behavior.
