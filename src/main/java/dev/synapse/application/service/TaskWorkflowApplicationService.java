package dev.synapse.application.service;

import dev.synapse.application.port.out.AgenticExecutionPort;
import dev.synapse.application.port.out.CreatePullRequestPort;
import dev.synapse.application.port.out.GitRepositoryPort;
import dev.synapse.application.port.out.JiraEnrichmentPort;
import dev.synapse.application.port.out.LoadTaskPort;
import dev.synapse.application.port.out.SaveTaskPort;
import dev.synapse.application.port.out.WorkspaceProvisioningPort;
import dev.synapse.domain.agent.AgentExecutionRequest;
import dev.synapse.domain.agent.AgentExecutionResult;
import dev.synapse.domain.event.TaskSubmittedEvent;
import dev.synapse.domain.task.EnrichedTaskContext;
import dev.synapse.domain.task.ParsedTaskPrompt;
import dev.synapse.domain.task.Task;
import dev.synapse.domain.task.TaskStatus;
import dev.synapse.domain.workspace.ProvisionedWorkspace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskWorkflowApplicationService {

    private final LoadTaskPort loadTaskPort;
    private final SaveTaskPort saveTaskPort;
    private final JiraEnrichmentPort jiraEnrichmentPort;
    private final WorkspaceProvisioningPort workspaceProvisioningPort;
    private final GitRepositoryPort gitRepositoryPort;
    private final AgenticExecutionPort agenticExecutionPort;
    private final CreatePullRequestPort createPullRequestPort;

    public void handleSubmitted(TaskSubmittedEvent event) {
        log.info("Handling TaskSubmittedEvent for taskId: {}", event.taskId().getValue());
        Task task = loadTaskPort.findById(event.taskId());

        task.startProcessing();
        saveTaskPort.save(task);

        ParsedTaskPrompt parsedPrompt = ParsedTaskPrompt.parse(task.getMessage().value());

        EnrichedTaskContext enrichedContext = jiraEnrichmentPort.enrichTask(parsedPrompt.ticketKey());
        log.info("Enriched Jira Task Context for ticketKey: {}", enrichedContext.ticketKey());

        String targetRepositoryUrl = parsedPrompt.repositoryUrl()
                .orElse(enrichedContext.targetRepositoryUrl());

        ProvisionedWorkspace provisionedWorkspace = workspaceProvisioningPort.provisionWorkspace(
                task.getId().getValue(),
                targetRepositoryUrl
        );
        log.info("Provisioned workspace at: {}", provisionedWorkspace.workspacePath());

        Path workspacePath = Path.of(provisionedWorkspace.workspacePath());
        gitRepositoryPort.cloneAndCheckout(
                targetRepositoryUrl,
                workspacePath,
                provisionedWorkspace.branchName()
        );

        AgentExecutionRequest executionRequest = new AgentExecutionRequest(
                task.getId().getValue(),
                parsedPrompt.cleanPrompt() + " " + enrichedContext.description(),
                provisionedWorkspace,
                "./gradlew test"
        );
        AgentExecutionResult executionResult = agenticExecutionPort.execute(executionRequest);
        log.info("Agentic execution finished with success [{}] for taskId: {}",
                executionResult.success(), task.getId().getValue());

        if (executionResult.success()) {
            finalizeSuccessfulTask(task, parsedPrompt, enrichedContext, provisionedWorkspace,
                    targetRepositoryUrl, executionResult);
        } else {
            task.fail();
        }
        saveTaskPort.save(task);
    }

    private void finalizeSuccessfulTask(
            Task task,
            ParsedTaskPrompt parsedPrompt,
            EnrichedTaskContext enrichedContext,
            ProvisionedWorkspace workspace,
            String targetRepositoryUrl,
            AgentExecutionResult executionResult
    ) {
        String ticketKey = resolveTicketKey(task, enrichedContext);
        String titleSummary = resolveTitleSummary(parsedPrompt, enrichedContext);

        String commitMessage = buildCommitMessage(task, parsedPrompt, enrichedContext, executionResult, ticketKey, titleSummary);
        gitRepositoryPort.commitAndPush(Path.of(workspace.workspacePath()), commitMessage);

        String prTitle = "feat(" + ticketKey + "): " + titleSummary;
        String prBody = buildPullRequestBody(task, parsedPrompt, enrichedContext, executionResult, titleSummary);

        String prUrl = createPullRequestPort.createPullRequest(targetRepositoryUrl, workspace.branchName(), prTitle, prBody);
        log.info("Pull Request created successfully [{}] for taskId: {}", prUrl, task.getId().getValue());
        if (prUrl != null && !prUrl.isBlank()) {
            task.attachPullRequestUrl(prUrl);
        }
        task.complete();
    }

    private String resolveTicketKey(Task task, EnrichedTaskContext enrichedContext) {
        return enrichedContext.ticketKey() != null && !enrichedContext.ticketKey().isBlank()
                ? enrichedContext.ticketKey()
                : task.getId().getValue();
    }

    private String resolveTitleSummary(ParsedTaskPrompt parsedPrompt, EnrichedTaskContext enrichedContext) {
        return enrichedContext.summary() != null && !enrichedContext.summary().isBlank()
                ? enrichedContext.summary()
                : parsedPrompt.cleanPrompt();
    }

    private String buildCommitMessage(
            Task task,
            ParsedTaskPrompt parsedPrompt,
            EnrichedTaskContext enrichedContext,
            AgentExecutionResult executionResult,
            String ticketKey,
            String titleSummary
    ) {
        StringBuilder commitMsgBuilder = new StringBuilder();
        commitMsgBuilder.append("feat(").append(ticketKey).append("): ").append(titleSummary).append("\n\n");
        commitMsgBuilder.append("Task ID: ").append(task.getId().getValue()).append("\n");
        commitMsgBuilder.append("Requester: ").append(task.getRequester().name()).append(" (").append(task.getSource().value()).append(")\n\n");
        commitMsgBuilder.append("User Request:\n").append(parsedPrompt.cleanPrompt()).append("\n\n");
        if (enrichedContext.description() != null && !enrichedContext.description().isBlank()) {
            commitMsgBuilder.append("Ticket Description:\n").append(enrichedContext.description()).append("\n\n");
        }
        commitMsgBuilder.append("Execution Summary:\n").append(executionResult.summary());
        return commitMsgBuilder.toString();
    }

    private String buildPullRequestBody(
            Task task,
            ParsedTaskPrompt parsedPrompt,
            EnrichedTaskContext enrichedContext,
            AgentExecutionResult executionResult,
            String titleSummary
    ) {
        StringBuilder prBodyBuilder = new StringBuilder();
        prBodyBuilder.append("## Autonomous Pull Request via Synapse\n\n");
        prBodyBuilder.append("### Task & Ticket Information\n");
        prBodyBuilder.append("- **Task ID**: `").append(task.getId().getValue()).append("`\n");
        if (enrichedContext.ticketKey() != null && !enrichedContext.ticketKey().isBlank()) {
            prBodyBuilder.append("- **Jira Ticket**: `").append(enrichedContext.ticketKey()).append("`\n");
        }
        prBodyBuilder.append("- **Summary**: ").append(titleSummary).append("\n");
        prBodyBuilder.append("- **Requester**: `").append(task.getRequester().name()).append("` via `").append(task.getSource().value()).append("`\n\n");
        prBodyBuilder.append("### User Request / Prompt\n");
        prBodyBuilder.append("```\n").append(parsedPrompt.cleanPrompt()).append("\n```\n\n");
        if (enrichedContext.description() != null && !enrichedContext.description().isBlank()) {
            prBodyBuilder.append("### Ticket Description\n");
            prBodyBuilder.append(enrichedContext.description()).append("\n\n");
        }
        if (enrichedContext.acceptanceCriteria() != null && !enrichedContext.acceptanceCriteria().isEmpty()) {
            prBodyBuilder.append("### Acceptance Criteria\n");
            for (String ac : enrichedContext.acceptanceCriteria()) {
                prBodyBuilder.append("- [x] ").append(ac).append("\n");
            }
            prBodyBuilder.append("\n");
        }
        prBodyBuilder.append("### Execution Summary & Changes\n");
        prBodyBuilder.append(executionResult.summary()).append("\n\n");
        prBodyBuilder.append("### Agent Diagnostic Logs\n");
        prBodyBuilder.append("<details>\n<summary>Click to expand execution logs</summary>\n\n```\n");
        prBodyBuilder.append(executionResult.logs());
        prBodyBuilder.append("\n```\n</details>");
        return prBodyBuilder.toString();
    }

    public void handleFailedSubmission(Task.TaskId taskId, String reason) {
        log.warn("Handling failed task submission for taskId: {}, reason: {}", taskId.getValue(), reason);
        Task task = loadTaskPort.findById(taskId);
        if (task != null && task.getStatus() != TaskStatus.FAILED
                && task.getStatus() != TaskStatus.COMPLETED) {
            task.fail();
            saveTaskPort.save(task);
        }
    }
}
