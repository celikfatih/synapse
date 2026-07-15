# Security Policy

The Synapse core engineering team takes security and container sandbox isolation very seriously. We appreciate your efforts to responsibly disclose your findings and will make every effort to acknowledge your contribution promptly.

---

## Supported Versions

Only the latest active release train of Synapse targeting **JDK 25+** receives active security updates and vulnerability patches.

| Version | Java Target | Supported |
| :--- | :--- | :--- |
| **0.0.x (`main`)** | **Java 25** | :white_check_mark: Yes |
| **< 0.0.1 (Legacy / Java 21)** | **Java 21** | :x: No |

---

## Reporting a Vulnerability

If you discover a security vulnerability, sandbox escape, or improper token handling issue within Synapse, **DO NOT create a public GitHub issue**. Public disclosure of security flaws prior to remediation puts existing deployments at risk.

Instead, please report the vulnerability privately via email to the project maintainer:
* **Maintainer Contact**: Fatih Celik
* **Private Disclosure Email**: [`celikfatih@protonmail.com`](mailto:celikfatih@protonmail.com)

### What to Include in Your Report
When submitting your private report, please provide:
1. **Description**: A summary of the vulnerability and potential impact (e.g., container escape, token leakage, unauthorized outbox tampering).
2. **Steps to Reproduce**: Minimal reproduction steps, sample payload (`SubmitTaskCommand`), or proof-of-concept scripts.
3. **Environment**: Version of Synapse (`main` commit hash), Docker version, and Java build specification.
4. **Suggested Remediation** (if known): Any code or configuration recommendations to remediate the issue.

### Our Response & Disclosure Timeline
* **Acknowledgement**: We will acknowledge receipt of your private vulnerability report within **48 hours**.
* **Triage & Assessment**: We will verify the vulnerability and assign a priority/severity rating within **5 business days**.
* **Remediation & Patch**: We aim to release a verified security fix within **14 days** of confirmation, depending on complexity and testing requirements.
* **Public Disclosure & Credit**: Once the patch is published and deployed, we will issue a public GitHub Security Advisory crediting your discovery (unless you request anonymity).

---

## AI Agent & Sandbox Security Boundaries

Because Synapse executes automated code modifications and shell verification commands (`--test-cmd`) via headless AI (`aider`), we enforce strict multi-layered security boundaries by design:

1. **Non-Root Ephemeral Sandbox Execution**:
   AI coding agents (`DockerAiderExecutionAdapter`) run exclusively inside unprivileged Docker container sandboxes (`synapse-sandbox:java25`, UID `1000`) with strict CPU/memory quotas (`8g`, `4 CPUs`). Never run Synapse or its worker adapters as `root` on host bare-metal systems.
2. **Control Plane Git Ownership**:
   Headless Aider executes with `--no-auto-commits`. The AI inside the sandbox cannot push arbitrary commits directly to remote origins. Synapse's control plane (`GitRepositoryPort` / `JGitRepositoryAdapter`) validates the test outcome before signing and pushing commits using isolated HTTPS token credentials (`SYNAPSE_GIT_TOKEN`).
3. **Correlation & Audit Logging**:
   Every incoming webhook (`Slack`, `REST`) generates a unique `CorrelationId` propagated through the Outbox, Kafka record headers, and container execution logs to ensure full cryptographic and operational traceability.

---

Thank you for helping keep Synapse and our community safe!
