# Contributing to Synapse

Welcome to **Synapse**! We are thrilled that you are interested in contributing to our autonomous engineering platform. Whether you are fixing bugs, improving documentation, introducing new outbound ports, or enhancing our AI coding sandbox capabilities, your contributions help shape the future of self-driving software development.

This document serves as the canonical guide for setting up your development environment, understanding our architectural patterns, running verification suites, and submitting high-quality Pull Requests (PRs).

---

## Table of Contents

1. [Our Vision & Philosophy](#1-our-vision--philosophy)
2. [Code of Conduct](#2-code-of-conduct)
3. [Architectural Principles & Design Rules](#3-architectural-principles--design-rules)
   - [Hexagonal Architecture (Ports & Adapters)](#hexagonal-architecture-ports--adapters)
   - [Transactional Outbox Pattern & Event-Driven Kafka](#transactional-outbox-pattern--event-driven-kafka)
   - [Domain-Driven Design (DDD) & Immutability](#domain-driven-design-ddd--immutability)
4. [Prerequisites & Environment Setup](#4-prerequisites--environment-setup)
   - [System Requirements](#system-requirements)
   - [Local Infrastructure (Docker Compose)](#local-infrastructure-docker-compose)
   - [Environment Variables & Configuration](#environment-variables--configuration)
5. [Building & Testing the Project](#5-building--testing-the-project)
   - [Running Unit & Integration Tests](#running-unit--integration-tests)
   - [Building the Application Bundle](#building-the-application-bundle)
   - [Building the Docker Agent Sandbox Image](#building-the-docker-agent-sandbox-image)
6. [Development Workflow & Pull Request Guidelines](#6-development-workflow--pull-request-guidelines)
   - [Branching & Conventional Commits](#branching--conventional-commits)
   - [Submitting a Pull Request](#submitting-a-pull-request)
   - [Code Review Process](#code-review-process)
7. [Security Guidelines & Vulnerability Disclosure](#7-security-guidelines--vulnerability-disclosure)

---

## 1. Our Vision & Philosophy

**Synapse** (`/ˈsɪnæps/`) is the critical junction where a conversation or alert sparks an autonomous, verifiable action in the codebase. Our goal is to bridge external alerts (Slack, REST APIs, Jira) with reliable, self-driving code modification via headless AI engines (**Aider**) inside secure, isolated Docker sandboxes.

When contributing, prioritize:
- **Reliability over cleverness**: Asynchronous workflows must be resilient to crashes, broker restarts, and network timeouts.
- **Strict Isolation**: AI coding agents should never execute directly on host systems without sandbox enforcement.
- **Auditability & Traceability**: Every transition must be tracked with correlation IDs across REST inputs, MongoDB outbox records, Kafka topics, and Slack notifications.

---

## 2. Code of Conduct

Our community follows the **Contributor Covenant** standards. We expect all contributors, maintainers, and users to:
- Be respectful, empathetic, and constructive in discussions, issues, and code reviews.
- Focus on what is best for the overall ecosystem and user experience.
- Gracefully give and receive feedback without personal attacks or harassment.

---

## 3. Architectural Principles & Design Rules

To maintain cleanliness and prevent technical debt, all changes must adhere strictly to our architectural boundaries.

### Hexagonal Architecture (Ports & Adapters)

Synapse strictly follows **Ports and Adapters**:
- **Domain & Application Core (`dev.synapse.domain.*`, `dev.synapse.application.*`)**: Contains domain models, state machines (`Task.java`), application use cases (`SubmitTaskUseCase`), and port interfaces (`Inbound/Outbound Ports`). **The Core must never depend on Spring MVC, Kafka frameworks, MongoDB drivers, JGit, or Docker APIs.**
- **Inbound Adapters (`dev.synapse.adapter.in.*`)**: REST controllers (`ApiTaskRestAdapter`, `SlackTaskRestAdapter`) and Kafka consumers (`KafkaDomainEventConsumerAdapter`). They convert external requests/records into clean domain commands/events and invoke inbound ports (`SubmitTaskUseCase`, `HandleDomainEventUseCase`).
- **Outbound Adapters (`dev.synapse.adapter.out.*`)**: Implementations of outbound ports:
  - `persistence/`: `MongoTaskStoreAdapter` (`SaveTaskPort`, `LoadTaskPort`), `MongoEventStoreAdapter` (`SaveDomainEventPort`, `LoadDomainEventPort`, `UpdateDomainEventPort`).
  - `messaging/`: `KafkaDomainEventPublisherAdapter` (`DomainEventPublisherPort`).
  - `workspace/`: `LocalGitWorkspaceProvisioningAdapter`, `DockerWorkspaceProvisioningAdapter` (`WorkspaceProvisioningPort`).
  - `git/`: `JGitRepositoryAdapter` (`GitRepositoryPort`).
  - `agent/`: `DockerAiderExecutionAdapter`, `AiderHeadlessExecutionAdapter` (`AgenticExecutionPort`).
  - `jira/`: `JiraRestAdapter` (`JiraEnrichmentPort`).

When adding a new external integration or persistence capability:
1. Define the interface contract in `application/port/out/` adhering strictly to the **`[Verb][DomainEntity]Port` naming convention** (e.g., `SaveTaskPort`, `LoadDomainEventPort`) and the **Interface Segregation Principle (ISP)** so application use cases only depend on the narrow methods they require.
2. Implement the adapter in `adapter/out/<provider>/`.
3. Wire configurations and properties in `shared/config/`.

### Transactional Outbox Pattern & Event-Driven Kafka

To guarantee at-least-once delivery without distributed two-phase commit locking:
- **Database & Outbox Atomicity**: Inbound tasks (`SubmitTaskUseCaseHandler`) MUST save the domain entity (`Task`) and write the corresponding domain event (`TaskSubmittedEvent` with `EventStatus.UNPUBLISHED`) to MongoDB within the same `@Transactional` method.
- **Outbox Polling (`PendingEventPublishJob`)**: A background scheduler periodically polls `UNPUBLISHED` events, dispatches them to Kafka (`synapse.kafka.task-topic`), and marks them as `PUBLISHED` upon broker acknowledgment.
- **Idempotency**: Because Kafka consumers may re-deliver messages, domain transition methods in `Task.java` (`startProcessing()`) must safely allow idempotent re-entry while strictly protecting terminal (`COMPLETED`) states.

### Domain-Driven Design (DDD) & Immutability

- **Never mutate domain objects arbitrarily**: Use domain behavior methods (`task.startProcessing()`, `task.complete()`, `task.fail()`) rather than public setters.
- **Immutability First**: Use `record` classes for commands, queries, events (`TaskSubmittedEvent`, `SubmitTaskCommand`), and data envelopes (`EnrichedTaskContext`).
- **Object Versioning**: Persistent MongoDB entities use `@Version Long version` to ensure proper optimistic locking and clean distinctions between `insertOne` and `replaceOne`.

---

## 4. Prerequisites & Environment Setup

### System Requirements

Before starting, ensure your local development environment meets the following specifications:
- **Java Development Kit (JDK) 25+** (Eclipse Temurin JDK 25 recommended)
- **Gradle 8+** (via the included `./gradlew` wrapper)
- **Docker & Docker Compose v2+** (required for local MongoDB, Kafka cluster, and AI sandboxes)
- **Git 2.40+**
- **Python 3.10+ & `uv`** (only required if developing/debugging local Aider Python environments outside Docker)

### Local Infrastructure (Docker Compose)

Synapse includes a complete local infrastructure bundle via `docker-compose.yml`. To start MongoDB, Apache Kafka broker, and Kafka UI:

```bash
# Start all local backing services in detached mode
docker compose up -d

# Check service status
docker compose ps
```

This starts:
- **MongoDB** on `localhost:27017` (database: `synapse`, credentials: `synapse-user` / `synapse12!`)
- **Apache Kafka (KRaft mode)** on `localhost:9092` / `localhost:29092`
- **Kafka UI** on `http://localhost:8082` (useful for inspecting topics and consumer groups)

### Environment Variables & Configuration (`.env` / `.env.example`)

Synapse uses `application.yml` with clean property placeholders (`${SYNAPSE_*}`). We provide a reference `.env.example` file at the root of the repository.

To configure your local environment:
1. Copy `.env.example` to `.env` (which is git-ignored to prevent accidental secret leaks):
   ```bash
   cp .env.example .env
   ```
2. Populate the required credentials and verify your local settings:

```env
# Core Persistence & Messaging (default values for local docker-compose)
SYNAPSE_MONGODB_URI=mongodb://synapse-user:synapse12!@localhost:27017/synapse?authSource=admin
SYNAPSE_KAFKA_SERVERS=localhost:29092
SYNAPSE_WORKSPACE_BASE_DIR=/tmp/synapse-workspaces
SYNAPSE_KAFKA_TASK_TOPIC=tasks
SYNAPSE_KAFKA_MAX_ATTEMPTS=3
SYNAPSE_KAFKA_BACKOFF_MS=1000

# External Integrations (Jira & Git Token Auth)
SYNAPSE_JIRA_BASE_URL=https://your-domain.atlassian.net
SYNAPSE_JIRA_USERNAME=your-email@example.com
SYNAPSE_JIRA_TOKEN=your-jira-api-token
SYNAPSE_GIT_TOKEN=your-github-personal-access-token
SYNAPSE_JIRA_PROJECT=PAY

# AI Coding Sandbox Engine (Headless Aider)
SYNAPSE_AGENT_DOCKER_IMAGE=synapse-sandbox:java25
SYNAPSE_AGENT_CONTAINER_WORKSPACE=/workspace
SYNAPSE_AGENT_MEMORY_LIMIT=8g
SYNAPSE_AGENT_CPUS=4
SYNAPSE_AGENT_TIMEOUT_SECONDS=1800
SYNAPSE_AGENT_MODEL=qwen2.5-coder:7b
SYNAPSE_AGENT_API_BASE_URL=http://localhost:8080
```

You can load your `.env` file directly using your IDE run configuration (IntelliJ IDEA / VS Code) or export the variables inside your shell before starting the application.

---

## 5. Building & Testing the Project

We enforce a **Test-Driven Development (TDD)** mindset. All code changes must be accompanied by comprehensive unit or integration tests (`JUnit 5`, `AssertJ`, and `Mockito`).

### Running Unit & Integration Tests and Code Coverage

Run the full verification suite across all adapters, use cases, and domain models alongside JaCoCo code coverage reporting:

```bash
./gradlew test jacocoTestReport
```

> **Note:** Our unit tests run cleanly offline without requiring live external credentials. After running the command, open `build/reports/jacoco/test/html/index.html` to inspect line and branch coverage across your modified packages.

### Building the Application Bundle

To compile the codebase, verify syntax, run tests, and generate the Spring Boot JAR:

```bash
./gradlew build
```

The resulting executable artifact will be produced in `build/libs/synapse-0.0.1-SNAPSHOT.jar`.

### Building the Docker Agent Sandbox Image

When modifying or testing headless AI execution (`DockerAiderExecutionAdapter`), ensure the local Docker sandbox image is built:

```bash
docker build -t synapse-sandbox:java25 -f Dockerfile.sandbox .
```

---

## 6. Development Workflow & Pull Request Guidelines

### Branching & Conventional Commits

1. Fork the repository and clone your fork locally:
   ```bash
   git clone https://github.com/your-username/synapse.git
   cd synapse
   ```
2. Create a focused feature or bugfix branch from `main`:
   ```bash
   git checkout -b feat/task-notification-port
   ```
3. Write clean, formatted Java code with appropriate comments and JavaDocs for public interfaces.
4. Follow **Conventional Commits** for all git commits (`<type>(<optional scope>): <description>`):
   - `feat: add Slack notification adapter for completed tasks`
   - `fix(jira): resolve null check when acceptance criteria are empty`
   - `refactor: extract workspace validation logic into dedicated domain service`
   - `test: verify outbox scheduler retry mechanisms`
   - `docs: update architecture overview diagram in README`

### Submitting a Pull Request

Before pushing your branch and opening a Pull Request:
1. Ensure your code compiles cleanly and `./gradlew test` succeeds (`BUILD SUCCESSFUL`).
2. Verify that your changes do not introduce any hardcoded secrets, API keys, or personal paths (`/Users/username/...`).
3. Push your branch to your fork:
   ```bash
   git push origin feat/task-notification-port
   ```
4. Open a Pull Request on GitHub against `synapse:main`.

**Your PR description should include:**
- **Summary of Changes**: What problem does this solve, and how does it fit into the Hexagonal Architecture?
- **Related Issues / Tickets**: Link any GitHub issue or discussion (`Closes #42`).
- **Test Plan**: Describe how you verified your changes (e.g., unit tests added, manual Docker sandbox execution log snippets).
- **Architectural Impact**: Confirm whether any new ports, adapters, or configuration properties were introduced.

### Code Review Process

- Maintainers will review your PR for architectural boundary compliance, domain cleanliness, security, and test coverage.
- Automated CI workflows will run `./gradlew test` and check build integrity.
- If changes are requested, push additional commits or interactive rebases to your feature branch; the PR will automatically update.

---

## 7. Security Guidelines & Vulnerability Disclosure

Security is fundamental to autonomous AI coding tools that clone repositories and execute shell commands inside containers.

- **Secrets Management**: NEVER commit `.env` files, API keys (`SYNAPSE_GIT_TOKEN`, `SYNAPSE_JIRA_TOKEN`), passwords, or private SSH keys. All sensitive configurations must use Spring property placeholders (`${...}`).
- **Sandbox Quotas & Isolation**: Never bypass CPU/memory limits or volume mount restrictions inside `DockerAiderExecutionAdapter`. Containers must remain strictly isolated.
- **Reporting Vulnerabilities**: If you discover a security issue or vulnerability (e.g., container escape, command injection via Jira prompts, or token leakage in logs), **DO NOT create a public GitHub issue**. Instead, report it privately to the project maintainers via email or GitHub private vulnerability reporting. We will investigate and coordinate a patch immediately.

---

Thank you for contributing to **Synapse** and helping build reliable, verifiable AI engineering systems!
