<div align="center">

# Synapse

**Autonomous AI Engineering Platform Bridging Alerts to Self-Driving Code Modifications inside Isolated Docker Sandboxes**

[![CI / Verification Pipeline](https://github.com/celikfatih/synapse/actions/workflows/ci.yml/badge.svg)](https://github.com/celikfatih/synapse/actions/workflows/ci.yml)
[![Test Coverage: JaCoCo](https://img.shields.io/badge/Test_Coverage-JaCoCo-008080.svg)](#-test-coverage--verification)
[![Java 25](https://img.shields.io/badge/Java-25%2B-ed8b00?style=flat&logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.0-6db33f?style=flat&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Architecture: Hexagonal](https://img.shields.io/badge/Architecture-Hexagonal_%2F_DDD-8a2be2?style=flat)](docs/ARCHITECTURE.md)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache_2.0-008080.svg)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-Welcome-brightgreen.svg)](CONTRIBUTING.md)
[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](CODE_OF_CONDUCT.md)

[Overview](#-why-synapse) • [Key Features](#-key-features) • [Quickstart](#-quickstart--5-minute-setup) • [Test Coverage](#-test-coverage--verification) • [Architecture](#-architecture--data-flow) • [Documentation](#-documentation--governance) • [Contributing](CONTRIBUTING.md)

</div>

---

> **syn·apse** (/ˈsɪnæps/) *noun*: In a biological system, a synapse is the junction where a signal is transmitted from one nerve cell to another.  
> *In this project: the critical junction where a developer alert or conversation sparks an autonomous, verifiable action in your repository.*

---

## Why Synapse?

Modern engineering teams face a continuous flood of alerts from monitoring systems, bug trackers (Jira), user chat channels (Slack), and REST webhooks. Traditionally, resolving these alerts requires a developer to manually triage the ticket, clone the repository, locate relevant symbols, write boilerplate fixes, run verification tests, and open a Pull Request.

**Synapse** automates this entire lifecycle. It serves as an enterprise-grade **AI Control Plane & Workflow Orchestrator**:
1. **Ingests alerts asynchronously** from Slack or REST APIs.
2. **Decouples processing** via an atomic **Transactional Outbox & Apache Kafka** pipeline.
3. **Enriches context** by querying Jira ticket acceptance criteria.
4. **Provisions isolated Docker sandboxes** (`synapse-sandbox:java25`) with strict CPU/memory quotas.
5. **Clones & checks out feature branches** (`feat/TASK-123`) securely via token authentication (`SYNAPSE_GIT_TOKEN`).
6. **Delegates autonomous coding** to headless **[Aider](https://aider.chat/)** (`aider --yes --test-cmd "./gradlew test"`), allowing the AI to index repository AST maps, edit files, run tests, and auto-heal compiler failures offline.
7. **Pushes verified commits & notifies teams** on Slack when the Pull Request is ready for review.

---

## Key Features

| Feature                                 | Description |
|:----------------------------------------| :--- |
| **Zero-Compromise Container Isolation** | AI coding agents execute inside non-root Docker sandboxes (`synapse-sandbox:java25`, UID 1000) with strict resource quotas (`8g` RAM, `4 CPUs`) and mounted dependency build caches. |
| **Strict Hexagonal Architecture**       | Clean separation of domain models, state transitions, and use cases (`dev.synapse.domain.*`) from external frameworks (Spring MVC, Kafka, MongoDB, JGit, Docker APIs). |
| **Transactional Outbox Pattern**        | Guaranteed at-least-once event delivery without two-phase commit overhead. Task state changes (`MongoDB`) and domain events are committed atomically before Kafka dispatch. |
| **Full Auditability & Tracing**         | End-to-end correlation (`correlationId`) powered by OpenTelemetry (`W3C Baggage`) and a decoupled `CorrelationIdRecordInterceptor` Anti-Corruption Layer, ensuring unbroken log traceability across REST, Slack, Kafka retries, and Docker sandboxes. |
| **AST Repo-Map & Self-Healing Tests**   | Integrates Aider's tree-sitter AST symbol indexer and `--test-cmd` verification loops. If a test fails after an edit, error outputs are automatically fed back to self-heal code. |

---

## Quickstart & 5-Minute Setup

### Prerequisites
* **JDK 25+** (Eclipse Temurin recommended)
* **Docker & Docker Compose v2+**
* **Gradle 8+** (included via `./gradlew`)

### 1. Start Local Backing Services
Spin up local MongoDB (`localhost:27017`), Apache Kafka broker (`localhost:29092`), and Kafka UI (`http://localhost:8082`):
```bash
docker compose up -d
```

### 2. Configure Environment Credentials
Copy the reference `.env.example` file and configure your API keys (`SYNAPSE_JIRA_TOKEN`, `SYNAPSE_GIT_TOKEN`):
```bash
cp .env.example .env
```

### 3. Build the AI Sandbox Docker Image
Pre-build the headless Aider execution environment (`synapse-sandbox:java25`) used during task execution:
```bash
docker build -t synapse-sandbox:java25 -f Dockerfile.sandbox .
```

### 4. Verify & Run the Platform
Run the offline-friendly unit test suite (`AssertJ`, `Mockito`, `JUnit 5`) alongside **JaCoCo code coverage**, then launch the Spring Boot server:
```bash
# Run verification suite and generate JaCoCo code coverage report
./gradlew test jacocoTestReport

# Start Synapse application server on port 8080
./gradlew bootRun
```

---

## Test Coverage & Verification

Synapse enforces strict test verification across domain state transitions, workflow orchestration, and outbox serialization before shipping code. We use **JaCoCo** (`jacoco`) integrated directly into our Gradle build pipeline and CI/CD workflows.

### Running & Viewing Coverage Reports
To execute unit tests and generate interactive HTML coverage reports locally during development:
```bash
./gradlew test jacocoTestReport
```
Once the task completes, open `build/reports/jacoco/test/html/index.html` in your web browser to inspect line and branch coverage across all modified packages and classes.

---

## Submitting Your First Autonomous Task

Once Synapse is running on `http://localhost:8080`, submit a task via REST API:

```bash
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "ticketKey": "PAY-1042",
    "repositoryUrl": "https://github.com/your-org/payment-service.git",
    "branch": "main",
    "prompt": "Fix null pointer exception in PaymentValidator when currency code is empty"
  }'
```

The server immediately returns an `ACCEPTED` task receipt with a `correlationId`. Synapse's background outbox scheduler (`PendingEventPublishJob`) dispatches the event to Kafka (`tasks` topic), where `TaskWorkflowApplicationService` enriches, provisions, clones, and executes the fix autonomously!

---

## Architecture & Data Flow

Synapse decouples high-throughput webhooks from resource-intensive container builds using an asynchronous, event-driven topology:

```mermaid
flowchart LR
    classDef ext fill:#f1f5f9,stroke:#475569,stroke-width:2px,color:#0f172a
    classDef core fill:#fef3c7,stroke:#d97706,stroke-width:2px,color:#0f172a
    classDef infra fill:#f3e8ff,stroke:#9333ea,stroke-width:2px,color:#0f172a

    Alert(["Slack / REST Webhook"]):::ext --> Ingest["Inbound REST Adapters (`dev.synapse.adapter.in.*`)"]:::core
    Ingest -->|"Atomic Tx"| Mongo[("MongoDB Outbox & Task Store")]:::infra
    Mongo -->|"Poll & Publish"| Kafka[("Kafka KRaft Event Broker")]:::infra
    Kafka --> Consumer["Kafka Consumer & Orchestrator (`TaskWorkflowApplicationService`)"]:::core
    Consumer --> Jira["Jira Enrichment Port (`JiraRestAdapter`)"]:::core
    Consumer --> Git["JGit Token Auth (`JGitRepositoryAdapter`)"]:::core
    Consumer --> Agent["Headless Aider Engine (`DockerAiderExecutionAdapter`)"]:::core
    Agent <--> Sandbox["Isolated Docker Sandbox (`synapse-sandbox:java25`)"]:::infra
```

**Want deep technical diagrams, state transition rules (`TaskStatus`), or Architectural Decision Records (ADRs)?**  
See our comprehensive technical reference: **[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)**.

---

## Documentation & Governance

We believe strong open-source communities are built on clear expectations, clean architecture, and mutual respect. Please review our governance documents:

* **[Contributing Guide (`CONTRIBUTING.md`)](CONTRIBUTING.md)**: Full local setup, Hexagonal Architecture guidelines, PR conventions, and verification steps.
* **[Architectural Reference (`docs/ARCHITECTURE.md`)](docs/ARCHITECTURE.md)**: C4 diagrams, sequence flows, AST Repo-Map specifics, and ADRs.
* **[Code of Conduct (`CODE_OF_CONDUCT.md`)](CODE_OF_CONDUCT.md)**: Our standards for an inclusive, welcoming community adopting the Contributor Covenant v2.1.
* **[License (`LICENSE`)](LICENSE)**: Synapse is open-source software licensed under the **Apache License 2.0**.
* **[Security Policy (`SECURITY.md`)](SECURITY.md)**: Private vulnerability disclosure process, supported version matrix, and AI sandbox isolation guarantees. To report security concerns directly, contact **[`celikfatih@protonmail.com`](mailto:celikfatih@protonmail.com)**.

---

<div align="center">
  Built with ❤️ for resilient, verifiable AI engineering.
</div>