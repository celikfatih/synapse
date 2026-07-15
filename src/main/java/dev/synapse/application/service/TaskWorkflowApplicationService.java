package dev.synapse.application.service;

import dev.synapse.application.port.out.AgenticExecutionPort;
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

        gitRepositoryPort.cloneAndCheckout(
                targetRepositoryUrl,
                Path.of(provisionedWorkspace.workspacePath()),
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
            task.complete();
        } else {
            task.fail();
        }
        saveTaskPort.save(task);
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
