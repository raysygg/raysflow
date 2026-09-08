# Backend Development Guide

## Responsibilities

The backend exposes the tenant-aware REST and WebSocket APIs used by the Vue frontend. It owns authorization, graph validation, draft/version persistence, workflow execution, RAG retrieval, connector invocation, metrics and audit records.

## Layering

```text
interfaces       HTTP/WebSocket adapters and DTOs
application      use cases, orchestration, execution and transactions
domain           business models and stable domain contracts
infrastructure   MyBatis mappers, node registry, RAG, model and connector adapters
config           Spring Security, JWT, Redis, task workers and runtime configuration
```

Controllers should translate transport data and delegate. Business decisions belong in application services. SQL access belongs in mappers. Do not put tenant filtering or workflow rules only in the frontend.

## Workflow lifecycle

1. `OrchestrationController` receives a draft graph.
2. `GraphContractValidator` normalizes legacy fields and validates graph topology, node schema, ports and variables.
3. `OrchestrationApplicationService` writes an immutable draft revision and its node/edge parts.
4. Publish validates executor availability and referenced resources, then writes an immutable version.
5. `orchestration_environment` points `PRODUCTION` to the active version.
6. `PersistentOrchestrationExecutionService` checks tenant, permission, resource authorization and idempotency before executing.
7. Execution context and node events are written during execution for recovery and observability.

## Adding a node type

1. Add the node descriptor and `configSchema` to the database seed/migration. Do not add a second hardcoded frontend catalog.
2. Add the executor key to the registry only when a real executor is available.
3. Implement the executor or connector adapter and persist node input/output/error events.
4. Add graph validation rules for required fields, ports and graph modes.
5. Add the frontend component only for interaction that cannot be represented by schema fields.
6. Run `scripts/validate-orchestration.ps1`, compile the backend and verify a published-version execution.

## Database

For a new database execute `sql/schema.sql`, then `sql/data.sql`. Existing environments must use versioned migrations. The application must not silently recreate a production database or reinsert baseline tenant data.

## Important contracts

- Every tenant-owned query must constrain `tenant_id`.
- Resource references use stable IDs or codes, never display names.
- Drafts are editable; published versions are immutable.
- Rollback changes an environment pointer and does not delete versions.
- Idempotency keys prevent duplicate business executions.
- Errors returned through `ApiResponse` must include a stable code and a user-readable message.
