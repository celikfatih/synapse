package dev.synapse.domain.task;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskStatusTransitionTest {

    @Test
    void new_Task_ShouldHaveStatusAccepted() {
        Task task = new Task("dev", "Fix bug", "corr-1", "SLACK");
        assertThat(task.getStatus()).isEqualTo(TaskStatus.ACCEPTED);
    }

    @Test
    void startProcessing_ShouldTransitionAcceptedToProcessing() {
        Task task = new Task("dev", "Fix bug", "corr-1", "SLACK");
        task.startProcessing();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PROCESSING);
    }

    @Test
    void complete_ShouldTransitionProcessingToCompleted() {
        Task task = new Task("dev", "Fix bug", "corr-1", "SLACK");
        task.startProcessing();
        task.complete();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void fail_ShouldTransitionNonTerminalStatesToFailed() {
        Task task1 = new Task("dev", "Fix bug", "corr-1", "SLACK");
        task1.fail();
        assertThat(task1.getStatus()).isEqualTo(TaskStatus.FAILED);

        Task task2 = new Task("dev", "Fix bug", "corr-1", "SLACK");
        task2.fail();
        assertThat(task2.getStatus()).isEqualTo(TaskStatus.FAILED);

        Task task3 = new Task("dev", "Fix bug", "corr-1", "SLACK");
        task3.startProcessing();
        task3.fail();
        assertThat(task3.getStatus()).isEqualTo(TaskStatus.FAILED);
    }

    @Test
    void startProcessing_ShouldAllowRetryFromProcessingAndFailedStates() {
        Task task = new Task("dev", "Fix bug", "corr-1", "SLACK");
        task.startProcessing();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PROCESSING);

        // idempotent retry while PROCESSING
        task.startProcessing();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PROCESSING);

        task.fail();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.FAILED);

        // retry after FAILED
        task.startProcessing();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.PROCESSING);
    }

    @Test
    void invalidTransitions_ShouldThrowIllegalStateException() {
        Task task = new Task("dev", "Fix bug", "corr-1", "SLACK");
        assertThatThrownBy(task::complete)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition to COMPLETED");

        task.startProcessing();
        task.complete();

        assertThatThrownBy(task::startProcessing)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition to PROCESSING from terminal status");

        assertThatThrownBy(task::fail)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot transition");
    }
}
