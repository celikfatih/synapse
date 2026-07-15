package dev.synapse.adapter.out.persistence;

import dev.synapse.adapter.out.persistence.entity.TaskDocument;
import dev.synapse.adapter.out.persistence.repository.MongoTaskRepository;
import dev.synapse.application.port.out.LoadTaskPort;
import dev.synapse.application.port.out.SaveTaskPort;
import dev.synapse.domain.task.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MongoTaskStoreAdapter implements SaveTaskPort, LoadTaskPort {

    private final MongoTaskRepository mongoTaskRepository;

    @Override
    public void save(Task task) {
        TaskDocument document = mongoTaskRepository.findById(task.getId().getValue())
                .orElseGet(() -> TaskDocument.builder()
                        .id(task.getId().getValue())
                        .build());

        document.setMessage(task.getMessage().value());
        document.setCorrelationId(task.getCorrelationId().value());
        document.setSource(task.getSource().value());
        document.setStatus(task.getStatus().name());
        document.setRequester(task.getRequester().name());

        mongoTaskRepository.save(document);
    }

    @Override
    public Task findById(Task.TaskId id) {
        Optional<TaskDocument> task = mongoTaskRepository.findById(id.getValue());
        return task.map(this::fromDomain)
                .orElseThrow(() -> new IllegalStateException("Task not found"));
    }

    private Task fromDomain(TaskDocument document) {
        return new Task(document.getId(),
                document.getRequester(),
                document.getMessage(),
                document.getSource(),
                document.getCorrelationId(),
                document.getStatus());
    }
}
