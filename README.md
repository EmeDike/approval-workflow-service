# Approval Workflow Service

A lightweight multi-level approval workflow REST API, built as a take-home assessment MVP for Infrest Systems. It allows departments to submit requests that must pass through one or more sequential approval levels before being marked approved or rejected.

## Tech Stack

- Java 21
- Spring Boot 3.3.4
- PostgreSQL 16
- Spring Data JPA / Hibernate
- Flyway (schema migrations)
- Maven
- Docker Compose (local Postgres)
- springdoc-openapi (Swagger UI)

## Project Structure

The codebase follows a standard layered architecture:

entity -> repository -> service -> controller, with dto and mapper packages keeping persistence models separate from the API contract, and an exception package handling domain errors and translating them into consistent HTTP responses.

- entity: JPA entities (ApprovalRequest, ApprovalStep) plus status enums
- dto: immutable request/response records with validation annotations
- mapper: converts entities to response DTOs
- repository: Spring Data JPA repository with a custom search query
- service: interface plus implementation containing the approval state machine
- controller: REST endpoints
- exception: custom exceptions plus a global exception handler

## Setup Instructions

### Prerequisites

- JDK 21
- Docker Desktop (for Postgres)
- IntelliJ IDEA or any IDE with Maven support (no separate Maven install required, the IDE bundles its own)

### Running locally

1. Start Postgres via Docker Compose:
   docker compose up -d

2. Run the application:
   Run ApprovalWorkflowServiceApplication from your IDE
   Flyway automatically applies migrations on startup.

3. API base URL: http://localhost:8080

4. Swagger UI: http://localhost:8080/swagger-ui.html
   Raw OpenAPI spec: http://localhost:8080/v3/api-docs

### Configuration

Active profile: local (set in application.yml). Datasource connection details live in application-local.yml.

### Running tests

Right-click src/test/java in your IDE and select Run All Tests.

Test coverage spans three layers, 18 tests in total:
- Service-layer unit tests (Mockito) covering the approval and rejection state machine
- Controller slice tests (WebMvcTest) covering HTTP status codes and validation
- Repository tests (DataJpaTest) covering the custom search query, requires Postgres running via Docker Compose

## API Endpoints

POST /api/approval-requests - Create a new approval request
GET /api/approval-requests/{id} - Get a request by ID
GET /api/approval-requests?status=&department= - Search and filter requests, paginated
POST /api/approval-requests/{id}/approve - Approve the current pending level
POST /api/approval-requests/{id}/reject - Reject the request at the current level

### Example: Create a request

POST /api/approval-requests
Content-Type: application/json

{
  "title": "New laptop",
  "description": "Dev needs a new machine",
  "department": "Engineering",
  "requesterName": "Ada",
  "approvers": [
    { "approverName": "Grace" },
    { "approverName": "Linus" }
  ]
}

Response: 201 Created, Location: /api/approval-requests/{id}

{
  "id": "b1a2c3d4-...",
  "title": "New laptop",
  "status": "PENDING",
  "currentLevel": 1,
  "version": 0,
  "approvalSteps": [
    { "levelNumber": 1, "approverName": "Grace", "status": "PENDING" },
    { "levelNumber": 2, "approverName": "Linus", "status": "PENDING" }
  ]
}

### Example: Approve current level

POST /api/approval-requests/{id}/approve
Content-Type: application/json

{
  "actorName": "Grace",
  "comments": "Looks good"
}

Response: 200 OK. Request status becomes IN_PROGRESS, currentLevel advances to 2. Approving the final level sets status to APPROVED.

### Example: Reject

POST /api/approval-requests/{id}/reject
Content-Type: application/json

{
  "actorName": "Grace",
  "comments": "Missing budget approval"
}

Response: 200 OK. Request status becomes REJECTED, terminal regardless of level.

### Example: Error response, wrong approver

{
  "timestamp": "2026-07-06T02:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Level 1 must be actioned by 'Grace', not 'WrongPerson'",
  "path": "/api/approval-requests/{id}/approve"
}

## Design Decisions

- Sequential multi-level approval: each ApprovalRequest owns an ordered list of ApprovalStep records, one per level. Only the step matching currentLevel is actionable, modeling a real escalation chain rather than parallel sign-offs.
- Rejection is terminal: rejecting at any level immediately ends the workflow rather than falling through to lower levels.
- DTOs as Java records, separate from entities: immutable, boilerplate-free contracts that do not leak persistence details such as Hibernate proxies or lazy collections over the wire.
- Optimistic locking, via a version field, protects against two approvers acting on the same request concurrently. Conflicts return 409 Conflict via a dedicated exception handler rather than a raw stack trace.
- Global exception handling centralizes all error-to-HTTP-status mapping, ensuring consistent JSON error bodies across the API instead of ad-hoc handling per endpoint.
- An entity graph on findWithStepsById avoids N+1 queries when loading a request with its steps, since steps are always needed together with the parent request in this API's usage pattern.
- Flyway over Hibernate auto-DDL: schema is explicit, versioned, and reviewable in migration files, with ddl-auto set to validate ensuring entities never silently drift from the real schema.
- Actor identified by name string, not a user ID: approvers are matched by name against the assigned step. See Assumptions below.

## Assumptions

- No authentication or authorization system was in scope for this MVP. The actorName field is a plain string compared against the assigned approver's name; it is not a verified identity. In a production system this would be backed by a real user or auth model, with actorName derived from an authenticated principal rather than submitted by the client.
- Approval levels are fixed at creation time and cannot be added, removed, or reordered after a request is submitted.
- A request is only actionable while PENDING or IN_PROGRESS. APPROVED and REJECTED are terminal states; further approve or reject calls against them return 409 Conflict.
- Department and requester are plain strings, not foreign keys to separate Department or User tables, since no such entities were defined in the assessment's scope.
- Approver names are matched case-insensitively to avoid brittle failures from minor casing differences, given there is no real identity system yet.

## Design Trade-offs

- String-based actor matching versus a full auth system: chosen for MVP speed given the assessment scope explicitly excludes auth. The trade-off is that this is not secure for real deployment and would need replacing before production use.
- Optimistic locking versus pessimistic locking: optimistic locking was chosen over a SELECT FOR UPDATE approach since approval actions are infrequent and low-contention in this domain. Pessimistic locking would add unnecessary throughput cost for a workflow that is not high-concurrency by nature.
- A JPQL query with nullable parameters versus a Specification or Querydsl approach: a single parameterized query was sufficient for two optional filters, status and department. A full Specification API would be over-engineering at this scale but would be the right call if filter criteria grew.
- A synchronous REST API versus an event-driven workflow: a synchronous request and response model was chosen for simplicity and to match the assessment's scope. A real enterprise workflow engine handling notifications, SLAs, or delegation would likely benefit from an event-driven architecture instead.

## Database Schema

See src/main/resources/db/migration:

- V0__enable_pgcrypto.sql enables UUID generation.
- V1__create_approval_workflow_tables.sql creates approval_requests and approval_steps, with a foreign key and cascade delete, a unique constraint on approval_request_id and level_number to prevent duplicate levels, and indexes on status and department to support the search endpoint.