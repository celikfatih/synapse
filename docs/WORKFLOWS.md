# Synapse Operational & Developer Workflows

This document serves as the canonical reference for all core operational, ingestion, execution, and recovery workflows across the **Synapse Autonomous AI Engineering Platform**.

---

## 1. End-to-End Autonomous Task Execution Workflow

When a task is submitted via REST or Slack, Synapse orchestrates an asynchronous, verifiable development lifecycle inside an isolated container sandbox before publishing code to GitHub.

```mermaid
sequenceDiagram
    autonumber
    actor Dev as Developer / Alert
    participant Ingest as REST / Slack Adapter
    participant Outbox as MongoDB Outbox
    participant Kafka as Kafka KRaft Topic
    participant Consumer as TaskWorkflowAppService
    participant Jira as JiraEnrichmentPort
    participant Git as GitRepositoryPort
    participant Sandbox as WorkspaceProvisioningPort
    participant Aider as Headless Aider Engine
    participant PR as CreatePullRequestPort
    participant Notify as SendNotificationPort

    Dev->>Ingest: Submit Task (ticketKey, repoUrl, branch, prompt)
    Ingest->>Outbox: Save Task (ACCEPTED) & TaskSubmittedEvent (Tx)
    Outbox-->>Kafka: PendingEventPublishJob dispatches event
    Kafka->>Consumer: Consume TaskSubmittedEvent
    Consumer->>Consumer: Transition status to PROCESSING
    Consumer->>Jira: enrichTask(ticketKey)
    Jira-->>Consumer: Enriched title & acceptance criteria

    Consumer->>Git: validateRepositoryExists(repoUrl)
    Note over Git: Pre-flight handshake via git ls-remote over HTTPS
    Git-->>Consumer: Repository accessible & credentials valid

    Consumer->>Sandbox: provisionWorkspace(taskId)
    Sandbox-->>Consumer: Local path (/workspaces/task-id) & volume mounts

    Consumer->>Git: cloneAndCheckout(repoUrl, workspacePath, branch)
    Consumer->>Aider: execute(AgentExecutionRequest)
    Note over Aider: Runs aider --yes --test-cmd "./gradlew test"<br/>Tree-sitter AST symbol indexing & auto-test self-healing
    Aider-->>Consumer: AgentExecutionResult (success, summary, logs)

    alt Verification Passed inside Sandbox
        Consumer->>Git: commitAndPush(workspacePath, branch, commitMsg)
        Consumer->>PR: createPullRequest(repoUrl, branch, title, body)
        Note over PR: Dynamically resolves default_branch (GET /repos/owner/repo)<br/>Opens PR via POST /repos/owner/repo/pulls
        PR-->>Consumer: Pull Request HTML URL (https://github.com/org/repo/pull/42)
        Consumer->>Consumer: Transition status to COMPLETED (attach PR URL)
        Consumer->>Notify: notifySuccess(taskId, prUrl, diffSummary)
    else Verification Failed / Build Broken
        Consumer->>Consumer: handleFailedSubmission(taskId, errorLogs)
        Consumer->>Notify: notifyFailure(taskId, diagnosticLogs)
    end
```

---

## 2. Slack Ingestion, Event Deduplication & Retry Protection Workflow

To ensure reliable alert ingestion without duplicate coding tasks, `SlackTaskRestAdapter` and `SlackPayloadTranslator` enforce strict filtering rules at the edge.

```mermaid
flowchart TD
    classDef ext fill:#f1f5f9,stroke:#475569,stroke-width:2px,color:#0f172a
    classDef check fill:#fef3c7,stroke:#d97706,stroke-width:2px,color:#0f172a
    classDef pass fill:#dcfce7,stroke:#16a34a,stroke-width:2px,color:#0f172a
    classDef drop fill:#fee2e2,stroke:#dc2626,stroke-width:2px,color:#0f172a

    Webhook(["POST /api/slack/events"]):::ext --> HMAC["SlackSignatureVerificationFilter<br/>(Verify X-Slack-Signature HMAC-SHA256)"]:::check
    HMAC -->|"Signature Invalid"| Reject401["HTTP 401 Unauthorized"]:::drop
    HMAC -->|"Signature Valid"| UrlCheck{"Type == url_verification?"}:::check
    
    UrlCheck -->|"Yes"| ReturnChallenge["Return challenge token (HTTP 200 OK)"]:::pass
    UrlCheck -->|"No (Event Callback)"| RetryCheck{"X-Slack-Retry-Num header present?"}:::check

    RetryCheck -->|"Yes (Retried Delivery)"| IgnoreRetry["Log retry & return HTTP 200 OK<br/>(Prevents Duplicate Task)"]:::drop
    RetryCheck -->|"No (Initial Delivery)"| Translator["SlackPayloadTranslator.translateEvent()"]:::check

    Translator --> BotCheck{"bot_id != null or bot_message?"}:::check
    BotCheck -->|"Yes"| IgnoreBot["Optional.empty() (Ignore Bot Echo)"]:::drop
    BotCheck -->|"No"| TypeCheck{"Event Type?"}:::check

    TypeCheck -->|"app_mention"| StripMention["Strip @Synapse & Extract Clean Prompt"]:::pass
    TypeCheck -->|"message"| ChannelCheck{"Is Direct Message?<br/>(im or channel starts with D)"}:::check

    ChannelCheck -->|"No (Regular Channel C.../G...)"| IgnoreChannel["Optional.empty()<br/>(app_mention already handles explicit mentions)"]:::drop
    ChannelCheck -->|"Yes (DM)"| StripMention
    StripMention --> SubmitCommand["SubmitTaskCommand -> SubmitTaskUseCase"]:::pass
```

### Key Rules:
1. **HMAC Signature Verification**: Every Slack request must pass `SlackSignatureVerificationFilter` validation against `SYNAPSE_SLACK_SIGNING_SECRET`.
2. **Webhook Retry Filtering**: If network latency or Kafka processing exceeds Slack's 3-second timeout, Slack retries delivery (`X-Slack-Retry-Num`). Synapse acknowledges `200 OK` instantly to drop duplicates.
3. **Channel Mention vs. Direct Message Deduplication**: In public or private channels, explicit `@Synapse` tags trigger both `app_mention` and `message` events simultaneously. Synapse processes `app_mention` and drops the redundant `message` event. In Direct Messages (`im`), `message` events are processed cleanly.

---

## 3. Pre-Flight Repository Handshake & Dynamic Pull Request Workflow

Before spawning heavy Docker containers or executing AI reasoning loops, Synapse validates repository availability and target branch metadata.

### Pre-Flight Remote Reference Check (`validateRepositoryExists`)
1. **Trigger**: Immediately upon receiving a `TaskSubmittedEvent` inside `TaskWorkflowApplicationService.handleSubmitted()`.
2. **Action**: `GitRepositoryPort.validateRepositoryExists(repositoryUrl)` invokes JGit's `lsRemoteRepository()`, establishing a quick HTTPS handshake using `SYNAPSE_GIT_TOKEN`.
3. **Outcome**:
   - **Accessible**: Handshake returns remote references (`refs/heads/*`), allowing workspace provisioning to proceed.
   - **Inaccessible / Authentication Failure**: Handshake throws an exception (`RemoteRepositoryException` / `TransportException`). Synapse aborts execution immediately, transitions the task to `FAILED`, and sends a diagnostic Slack alert without spinning up Docker sandboxes.

### Dynamic Pull Request Target Resolution (`resolveBaseBranch`)
When `CreatePullRequestPort.createPullRequest(repoUrl, headBranch, title, body)` is called:
1. If `synapse.git.default-base-branch` is explicitly configured to a non-default branch (e.g., `develop` or `release/v2`), that branch is targeted directly.
2. If configured to `"main"` or left blank, `GitHubRestPullRequestAdapter.resolveBaseBranch()` sends a lightweight `GET https://api.github.com/repos/{owner}/{repo}` request authenticated with `Bearer SYNAPSE_GIT_TOKEN`.
3. The response `default_branch` (`main`, `master`, `trunk`, or `develop`) is extracted and passed directly to `POST /repos/{owner}/{repo}/pulls`, guaranteeing that pull requests open without `422 Unprocessable Content (base: invalid)` errors.

---

## 4. Resilient Failure Recovery & Dead-Letter Queue (DLQ) Workflow

Synapse protects internal state consistency across transient network glitches and terminal agent failures using Spring Kafka and MongoDB versioning.

```mermaid
stateDiagram-v2
    [*] --> ACCEPTED: SubmitTaskUseCase.submit()
    ACCEPTED --> PROCESSING: TaskWorkflowApplicationService.handleSubmitted()
    
    PROCESSING --> PROCESSING: Idempotent Retry / Consumer Re-delivery
    PROCESSING --> COMPLETED: Aider verification & PR creation success
    
    PROCESSING --> FAILED: Pre-flight repo check failure
    PROCESSING --> FAILED: Aider verification exhausted (--auto-test)
    PROCESSING --> FAILED: Kafka Dead-Letter Recoverer (Max Retries Exceeded)
    
    FAILED --> PROCESSING: Manual Re-submission / Recovery Retry
    COMPLETED --> [*]
```

### Recovery Mechanisms:
* **Kafka Exponential Backoff**: `DefaultErrorHandler` retries transient consumer failures (e.g., temporary database lock or network timeout) with exponential backoff.
* **Dead-Letter Recoverer (`TaskEventConsumerRecordRecoverer`)**: When maximum retry attempts are exhausted, `TaskEventConsumerRecordRecoverer` catches the record and invokes `TaskWorkflowApplicationService.handleFailedSubmission(taskId, reason)`.
* **MongoDB Version-Safe Updates**: `TaskDocument` enforces optimistic locking via `@Version Long version`. This ensures that state updates (`ACCEPTED -> FAILED` or `PROCESSING -> COMPLETED`) cleanly execute as `replaceOne` rather than generating `E11000 duplicate key` exceptions during concurrent retries.

---

## 5. Local Development & Offline TDD Workflow

Engineers working on Synapse can develop, test, and verify code locally without requiring external backing services or active GitHub connections.

### Step 1: Run Offline Unit & Integration Tests
Synapse's test suite uses `@Profile("test")` and `LocalGitWorkspaceProvisioningAdapter` to execute entirely on the local filesystem:
```bash
./gradlew test jacocoTestReport
```
View the generated JaCoCo coverage report at `build/reports/jacoco/test/html/index.html` (minimum 80% coverage required across domain state machines and adapters).

### Step 2: Build the AI Sandbox Image
When testing agentic container execution (`@Profile("prod")`), pre-build the headless Aider Docker image:
```bash
docker build -t synapse-sandbox:java25 -f Dockerfile.sandbox .
```

### Step 3: Run with Local Backing Services
Launch local MongoDB (`:27017`) and Kafka KRaft (`:29092`) via Docker Compose, then start Synapse:
```bash
# Start local MongoDB & Kafka
docker compose up -d

# Run Synapse application server
./gradlew bootRun
```
