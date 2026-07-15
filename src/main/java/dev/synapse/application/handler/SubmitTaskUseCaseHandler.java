package dev.synapse.application.handler;

import dev.synapse.application.port.in.SubmitTaskUseCase;
import dev.synapse.application.port.in.command.SubmitTaskCommand;
import dev.synapse.application.port.out.SaveDomainEventPort;
import dev.synapse.application.port.out.SaveTaskPort;
import dev.synapse.domain.event.TaskSubmittedEvent;
import dev.synapse.domain.task.Task;
import dev.synapse.shared.tracing.TracingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubmitTaskUseCaseHandler implements SubmitTaskUseCase {

    private final SaveTaskPort saveTaskPort;
    private final SaveDomainEventPort saveDomainEventPort;

    @Override
    @Transactional
    public void submit(SubmitTaskCommand command) {
        Task task = new Task(
                command.requester(),
                command.message(),
                TracingContext.getCorrelationId(),
                command.origin().channel());

        saveTaskPort.save(task);

        TaskSubmittedEvent event = new TaskSubmittedEvent(
                task.getId(),
                task.getMessage().value(),
                task.getSource().value());

        saveDomainEventPort.save(event);
        
        log.info("Task submitted: {} successfully", task);
    }
}
