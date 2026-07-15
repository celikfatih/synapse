## Summary of Changes

<!-- Provide a brief description of what this PR accomplished and why it is needed. -->


## Related Issues / Tickets

<!-- Link to any GitHub issue, discussion, or Jira ticket (e.g., Closes #42, Related to #15). -->
* Closes #

## Architectural & Layer Checklist

Please confirm that your changes adhere to Synapse's Hexagonal Architecture rules (`dev.synapse.*`):
- [ ] **Domain Core Purity**: Changes in `dev.synapse.domain.*` or `application.*` do not import Spring MVC, Kafka, MongoDB, JGit, or Docker APIs.
- [ ] **Port Interfaces**: If introducing a new external service, a clean interface was added to `application/port/out/` and implemented under `adapter/out/`.
- [ ] **Outbox & Atomicity**: If mutating task state (`SubmitTaskUseCaseHandler`), the domain object and domain event were saved within the same `@Transactional` boundary.
- [ ] **Immutability**: Domain commands, events, and data objects use `record`s or immutable structures.

## Verification & Test Plan

- [ ] `./gradlew test` runs locally and succeeds (`BUILD SUCCESSFUL`).
- [ ] New unit or integration tests (`JUnit 5`, `AssertJ`, `Mockito`) were added to cover the modified behavior.
- [ ] If modifying Docker sandbox / Aider execution, verified against a local build (`docker build -t synapse-sandbox:java25 -f Dockerfile.sandbox .`).

<!-- Describe any manual verification steps or log outputs below if applicable: -->


## Security Hygiene

- [ ] No hardcoded API keys (`SYNAPSE_GIT_TOKEN`, `SYNAPSE_JIRA_TOKEN`), personal paths (`/Users/username/...`), or `.env` files are included in this PR.
- [ ] All sensitive configurations use Spring property placeholders (`${...}`).
