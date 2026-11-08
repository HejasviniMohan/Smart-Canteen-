# ADR 1: Use Postgres everywhere, and drop H2

**Context.** Development and tests ran on H2, production on Postgres. The peak-hours query used `HOUR()`, which H2 has and Postgres doesn't. Everything passed locally, and the admin dashboard would have failed in production.

**Decision.** Run real Postgres everywhere: Docker Compose for development, Testcontainers for tests. Flyway owns the schema, and Hibernate only validates that entities match it (`ddl-auto=validate`).

**Trade-offs.** Tests need Docker and start more slowly (about 10 seconds). In return they use `ON CONFLICT`, `RETURNING` and partial indexes as production will, and every migration runs on every test run.
