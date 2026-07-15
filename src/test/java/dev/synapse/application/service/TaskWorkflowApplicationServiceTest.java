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
import dev.synapse.domain.task.Task;
import dev.synapse.domain.task.TaskStatus;
import dev.synapse.domain.workspace.ProvisionedWorkspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskWorkflowApplicationServiceTest {

    @Mock
    private LoadTaskPort loadTaskPort;

    @Mock
    private SaveTaskPort saveTaskPort;

    @Mock
    private JiraEnrichmentPort jiraEnrichmentPort;

    @Mock
    private WorkspaceProvisioningPort workspaceProvisioningPort;

    @Mock
    private GitRepositoryPort gitRepositoryPort;

    @Mock
    private AgenticExecutionPort agenticExecutionPort;

    @InjectMocks
    private TaskWorkflowApplicationService taskWorkflowApplicationService;

    @Test
    void handleSubmitted_ShouldOverrideJiraRepoUrl_WhenExplicitRepoFlagProvidedInPrompt() {
        // given
        String rawMessage = "PAY-1042 Fix refund --repo git@github.com:override/repo.git";
        Task task = new Task("developer", rawMessage, "corr-123", "SLACK");
        TaskSubmittedEvent event = new TaskSubmittedEvent(task.getId(), rawMessage, "SLACK");

        EnrichedTaskContext enrichedContext = new EnrichedTaskContext(
                "PAY-1042",
                "Summary",
                "Description",
                List.of("AC1"),
                "git@github.com:jira-default/repo.git"
        );

        ProvisionedWorkspace provisionedWorkspace = new ProvisionedWorkspace(
                task.getId().getValue(),
                "git@github.com:override/repo.git",
                "/tmp/synapse-workspaces/" + task.getId().getValue(),
                "feat/" + task.getId().getValue()
        );

        when(loadTaskPort.findById(event.taskId())).thenReturn(task);
        when(jiraEnrichmentPort.enrichTask("PAY-1042")).thenReturn(enrichedContext);
        when(workspaceProvisioningPort.provisionWorkspace(task.getId().getValue(), "git@github.com:override/repo.git"))
                .thenReturn(provisionedWorkspace);
        when(agenticExecutionPort.execute(any())).thenReturn(
                AgentExecutionResult.success(task.getId().getValue(), "Done", "logs")
        );

        // when
        taskWorkflowApplicationService.handleSubmitted(event);

        // then
        verify(loadTaskPort).findById(event.taskId());
        verify(jiraEnrichmentPort).enrichTask("PAY-1042");
        verify(workspaceProvisioningPort).provisionWorkspace(task.getId().getValue(), "git@github.com:override/repo.git");
        verify(agenticExecutionPort).execute(any(AgentExecutionRequest.class));
        verify(saveTaskPort, times(2)).save(task);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void handleSubmitted_ShouldUseJiraRepoUrl_WhenNoRepoFlagProvidedInPrompt() {
        // given
        String rawMessage = "PAY-1042 Fix refund";
        Task task = new Task("developer", rawMessage, "corr-123", "SLACK");
        TaskSubmittedEvent event = new TaskSubmittedEvent(task.getId(), rawMessage, "SLACK");

        EnrichedTaskContext enrichedContext = new EnrichedTaskContext(
                "PAY-1042",
                "Summary",
                "Description",
                List.of("AC1"),
                "git@github.com:jira-default/repo.git"
        );

        ProvisionedWorkspace provisionedWorkspace = new ProvisionedWorkspace(
                task.getId().getValue(),
                "git@github.com:jira-default/repo.git",
                "/tmp/synapse-workspaces/" + task.getId().getValue(),
                "feat/" + task.getId().getValue()
        );

        when(loadTaskPort.findById(event.taskId())).thenReturn(task);
        when(jiraEnrichmentPort.enrichTask("PAY-1042")).thenReturn(enrichedContext);
        when(workspaceProvisioningPort.provisionWorkspace(task.getId().getValue(), "git@github.com:jira-default/repo.git"))
                .thenReturn(provisionedWorkspace);
        when(agenticExecutionPort.execute(any())).thenReturn(
                AgentExecutionResult.success(task.getId().getValue(), "Done", "logs")
        );

        // when
        taskWorkflowApplicationService.handleSubmitted(event);

        // then
        verify(loadTaskPort).findById(event.taskId());
        verify(jiraEnrichmentPort).enrichTask("PAY-1042");
        verify(workspaceProvisioningPort).provisionWorkspace(task.getId().getValue(), "git@github.com:jira-default/repo.git");
        verify(gitRepositoryPort).cloneAndCheckout(
                "git@github.com:jira-default/repo.git",
                Path.of("/tmp/synapse-workspaces/" + task.getId().getValue()),
                "feat/" + task.getId().getValue()
        );
        verify(agenticExecutionPort).execute(any(AgentExecutionRequest.class));
        verify(saveTaskPort, times(2)).save(task);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void handleSubmitted_ShouldMarkTaskFailed_WhenAgentExecutionReturnsFailure() {
        // given
        String rawMessage = "PAY-1042 Fix refund";
        Task task = new Task("developer", rawMessage, "corr-123", "SLACK");
        TaskSubmittedEvent event = new TaskSubmittedEvent(task.getId(), rawMessage, "SLACK");

        EnrichedTaskContext enrichedContext = new EnrichedTaskContext(
                "PAY-1042", "Summary", "Description", List.of("AC1"), "git@github.com:jira-default/repo.git"
        );
        ProvisionedWorkspace provisionedWorkspace = new ProvisionedWorkspace(
                task.getId().getValue(), "git@github.com:jira-default/repo.git", "/tmp", "feat/1"
        );

        when(loadTaskPort.findById(event.taskId())).thenReturn(task);
        when(jiraEnrichmentPort.enrichTask("PAY-1042")).thenReturn(enrichedContext);
        when(workspaceProvisioningPort.provisionWorkspace(any(), any())).thenReturn(provisionedWorkspace);
        when(agenticExecutionPort.execute(any())).thenReturn(
                AgentExecutionResult.failure(task.getId().getValue(), "Failed", "logs")
        );

        // when
        taskWorkflowApplicationService.handleSubmitted(event);

        // then
        verify(saveTaskPort, times(2)).save(task);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
    }

    @Test
    void handleSubmitted_ShouldPropagateException_WhenExceptionOccurs() {
        // given
        String rawMessage = "PAY-1042 Fix refund";
        Task task = new Task("developer", rawMessage, "corr-123", "SLACK");
        TaskSubmittedEvent event = new TaskSubmittedEvent(task.getId(), rawMessage, "SLACK");

        when(loadTaskPort.findById(event.taskId())).thenReturn(task);
        when(jiraEnrichmentPort.enrichTask("PAY-1042")).thenThrow(new RuntimeException("Jira error"));

        // when / then
        assertThatThrownBy(() -> taskWorkflowApplicationService.handleSubmitted(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Jira error");

        verify(saveTaskPort, times(1)).save(task);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PROCESSING);
    }

    @Test
    void handleFailedSubmission_ShouldTransitionTaskToFailedAndSave_WhenTaskNotTerminal() {
        // given
        Task task = new Task("developer", "Fix refund", "corr-123", "SLACK");
        when(loadTaskPort.findById(task.getId())).thenReturn(task);

        // when
        taskWorkflowApplicationService.handleFailedSubmission(task.getId(), "Exceeded retries");

        // then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
        verify(saveTaskPort).save(task);
    }

    @Test
    void handleFailedSubmission_ShouldNotSave_WhenTaskAlreadyFailedOrCompleted() {
        // given
        Task task = new Task("developer", "Fix refund", "corr-123", "SLACK");
        task.fail();
        when(loadTaskPort.findById(task.getId())).thenReturn(task);

        // when
        taskWorkflowApplicationService.handleFailedSubmission(task.getId(), "Exceeded retries");

        // then
        assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);
        verify(saveTaskPort, times(0)).save(task);
    }
}
