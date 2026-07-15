package dev.synapse.application.event;

import dev.synapse.application.service.TaskWorkflowApplicationService;
import dev.synapse.domain.event.TaskSubmittedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskSubmittedEventProcessor implements DomainEventProcessor<TaskSubmittedEvent> {

    private final TaskWorkflowApplicationService taskWorkflowApplicationService;

    @Override
    public void process(TaskSubmittedEvent event) {
        taskWorkflowApplicationService.handleSubmitted(event);
    }

    @Override
    public Class<TaskSubmittedEvent> eventType() {
        return TaskSubmittedEvent.class;
    }
}
