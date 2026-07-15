package dev.synapse.application.port.in;

import dev.synapse.application.port.in.command.SubmitTaskCommand;

public interface SubmitTaskUseCase {
    /**
     * Submits a new task to the system.
     * @param command the command containing the task details.
     */
    void submit(SubmitTaskCommand command);
}
