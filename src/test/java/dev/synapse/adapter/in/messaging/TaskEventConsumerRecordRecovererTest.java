package dev.synapse.adapter.in.messaging;

import dev.synapse.application.service.TaskWorkflowApplicationService;
import dev.synapse.domain.event.TaskSubmittedEvent;
import dev.synapse.domain.task.Task;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TaskEventConsumerRecordRecovererTest {

    @Mock
    private TaskWorkflowApplicationService taskWorkflowApplicationService;

    @Mock
    private DeadLetterPublishingRecoverer deadLetterPublishingRecoverer;

    @InjectMocks
    private TaskEventConsumerRecordRecoverer recoverer;

    @Test
    void accept_ShouldDelegateToApplicationServiceAndDlq_WhenRecordIsTaskSubmittedEvent() {
        // given
        Task.TaskId taskId = Task.TaskId.generate();
        TaskSubmittedEvent event = new TaskSubmittedEvent(taskId, "Fix bug", "SLACK");
        ConsumerRecord<String, Object> eventRecord = new ConsumerRecord<>("tasks", 0, 100L, "key", event);
        Exception exception = new RuntimeException("Kafka delivery failed");

        // when
        recoverer.accept(eventRecord, exception);

        // then
        verify(taskWorkflowApplicationService).handleFailedSubmission(taskId, "Kafka delivery failed");
        verify(deadLetterPublishingRecoverer).accept(eventRecord, exception);
    }
}
