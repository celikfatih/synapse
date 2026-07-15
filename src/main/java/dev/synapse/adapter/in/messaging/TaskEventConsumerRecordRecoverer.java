package dev.synapse.adapter.in.messaging;

import dev.synapse.adapter.in.messaging.interceptor.CorrelationIdRecordInterceptor;
import dev.synapse.application.service.TaskWorkflowApplicationService;
import dev.synapse.domain.event.TaskSubmittedEvent;
import dev.synapse.shared.tracing.TracingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskEventConsumerRecordRecoverer implements ConsumerRecordRecoverer {

    private final TaskWorkflowApplicationService taskWorkflowApplicationService;

    @Override
    public void accept(ConsumerRecord<?, ?> eventRecord, Exception exception) {
        CorrelationIdRecordInterceptor.extractAndSet(eventRecord);
        try {
            log.error("Exceeded max retry attempts for record [{}]: {}", eventRecord.value(), exception.getMessage());
            if (eventRecord.value() instanceof TaskSubmittedEvent event) {
                taskWorkflowApplicationService.handleFailedSubmission(event.taskId(), exception.getMessage());
            }
        } finally {
            TracingContext.clearCorrelationId();
        }
    }
}
