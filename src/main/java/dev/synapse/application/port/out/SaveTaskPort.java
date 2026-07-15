package dev.synapse.application.port.out;

import dev.synapse.domain.task.Task;

public interface SaveTaskPort {
    /**
     * Saves a task to the system.
     * @param task the task to be saved.
     */
    void save(Task task);
}
