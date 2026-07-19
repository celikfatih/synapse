package dev.synapse.adapter.in.messaging;

import dev.synapse.adapter.in.messaging.interceptor.CorrelationIdRecordInterceptor;
import dev.synapse.application.service.TaskWorkflowApplicationService;
import dev.synapse.domain.event.TaskSubmittedEvent;
import dev.synapse.shared.tracing.TracingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskEventConsumerRecordRecoverer implements ConsumerRecordRecoverer {

    private final TaskWorkflowApplicationService taskWorkflowApplicationService;
    private final DeadLetterPublishingRecoverer deadLetterPublishingRecoverer;

    @Override
    public void accept(ConsumerRecord<?, ?> eventRecord, Exception exception) {
        CorrelationIdRecordInterceptor.extractAndSet(eventRecord);
        try {
            String errorMsg = exception.getCause() != null ? exception.getCause().getMessage() : exception.getMessage();
            log.error("Exceeded max retry attempts for record [{}]: {}", eventRecord.value(), errorMsg);
            if (eventRecord.value() instanceof TaskSubmittedEvent event) {
                taskWorkflowApplicationService.handleFailedSubmission(event.taskId(), errorMsg);
            }
            if (deadLetterPublishingRecoverer != null) {
                deadLetterPublishingRecoverer.accept(eventRecord, exception);
            }
        } finally {
            TracingContext.clearCorrelationId();
        }
    }
}
