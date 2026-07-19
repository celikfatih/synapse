package dev.synapse.application.service;

import dev.synapse.application.port.out.AgenticExecutionPort;
import dev.synapse.application.port.out.CreatePullRequestPort;
import dev.synapse.application.port.out.GitRepositoryPort;
import dev.synapse.application.port.out.JiraEnrichmentPort;
import dev.synapse.application.port.out.LoadTaskPort;
import dev.synapse.application.port.out.SaveTaskPort;
import dev.synapse.application.port.out.SendNotificationPort;
import dev.synapse.application.port.out.WorkspaceProvisioningPort;
import dev.synapse.domain.agent.AgentExecutionRequest;
import dev.synapse.domain.agent.AgentExecutionResult;
import dev.synapse.domain.agent.AgentWorkspaceExecution;
import dev.synapse.domain.event.TaskSubmittedEvent;
import dev.synapse.domain.notification.NotificationRequest;
import dev.synapse.domain.task.EnrichedTaskContext;
import dev.synapse.domain.task.ParsedTaskPrompt;
import dev.synapse.domain.task.Task;
import dev.synapse.domain.task.TaskStatus;
import dev.synapse.domain.workspace.ProvisionedWorkspace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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
    private final SendNotificationPort sendNotificationPort;

    public void handleSubmitted(TaskSubmittedEvent event) {
        log.info("Handling TaskSubmittedEvent for taskId: {}", event.taskId().getValue());
        Task task = loadTaskPort.findById(event.taskId());

        task.startProcessing();
        saveTaskPort.save(task);

        ParsedTaskPrompt parsedPrompt = ParsedTaskPrompt.parse(task.getMessage().value());

        EnrichedTaskContext enrichedContext = StringUtils.hasText(parsedPrompt.ticketKey())
                ? jiraEnrichmentPort.enrichTask(parsedPrompt.ticketKey())
                : EnrichedTaskContext.empty("");
        log.info("Enriched Jira Task Context for ticketKey: {}", enrichedContext.ticketKey());

        String targetRepositoryUrl = parsedPrompt.repositoryUrl()
                .orElse(enrichedContext.targetRepositoryUrl());

        if (targetRepositoryUrl == null || targetRepositoryUrl.isBlank()) {
            String errorMsg = "Task submission failed: Missing required target repository. Please include --repo <url> in your command or specify a valid Jira ticket with a configured repository.";
            log.error("Aborting execution for taskId [{}]: {}", task.getId().getValue(), errorMsg);
            finalizeFailedTask(task, AgentExecutionResult.failure(task.getId().getValue(), errorMsg, ""));
            saveTaskPort.save(task);
            return;
        }

        if (!gitRepositoryPort.validateRepositoryExists(targetRepositoryUrl)) {
            String errorMsg = "Task submission failed: Repository [" + targetRepositoryUrl + "] is invalid, inaccessible, or does not exist. Please check your --repo flag or repository credentials.";
            log.error("Aborting execution for taskId [{}]: {}", task.getId().getValue(), errorMsg);
            finalizeFailedTask(task, AgentExecutionResult.failure(task.getId().getValue(), errorMsg, ""));
            saveTaskPort.save(task);
            return;
        }

        AgentWorkspaceExecution execution = provisionAndExecuteAgent(task, parsedPrompt, enrichedContext, targetRepositoryUrl);

        if (execution.executionResult().success()) {
            finalizeSuccessfulTask(task, parsedPrompt, enrichedContext, execution.workspace(),
                    targetRepositoryUrl, execution.executionResult());
        } else {
            finalizeFailedTask(task, execution.executionResult());
        }
        saveTaskPort.save(task);
    }

    private AgentWorkspaceExecution provisionAndExecuteAgent(
            Task task,
            ParsedTaskPrompt parsedPrompt,
            EnrichedTaskContext enrichedContext,
            String targetRepositoryUrl
    ) {
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
        return new AgentWorkspaceExecution(provisionedWorkspace, executionResult);
    }

    private void finalizeFailedTask(Task task, AgentExecutionResult executionResult) {
        task.fail();
        sendNotificationPort.sendNotification(NotificationRequest.failure(
                task.getId().getValue(),
                task.getCorrelationId().value(),
                task.getRequester().name(),
                task.getSource().value(),
                "Agentic execution failed: " + executionResult.summary()
        ));
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
        String notificationSummary = executionResult.summary();
        if (prUrl != null && !prUrl.isBlank()) {
            task.attachPullRequestUrl(prUrl);
        } else {
            notificationSummary += "\n\n⚠️ Note: Pull Request could not be created automatically. Please check remote repository access or branch configuration.";
        }
        task.complete();
        sendNotificationPort.sendNotification(NotificationRequest.success(
                task.getId().getValue(),
                task.getCorrelationId().value(),
                task.getRequester().name(),
                task.getSource().value(),
                notificationSummary,
                prUrl
        ));
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
            sendNotificationPort.sendNotification(NotificationRequest.failure(
                    taskId.getValue(),
                    task.getCorrelationId().value(),
                    task.getRequester().name(),
                    task.getSource().value(),
                    reason
            ));
        }
    }
}
