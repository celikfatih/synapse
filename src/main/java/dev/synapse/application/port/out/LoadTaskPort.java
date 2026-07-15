package dev.synapse.application.port.out;

import dev.synapse.domain.task.Task;

public interface LoadTaskPort {
    /**
     * Finds a task by its ID.
     * @param id the ID of the task.
     * @return the task, or exception if not found.
     */
    Task findById(Task.TaskId id);
}
