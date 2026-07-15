package dev.synapse.application.port.in.command;

public record SubmitTaskCommand(String requester, String message, SubmissionOrigin origin) {
    public SubmitTaskCommand {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }
        if (requester == null || requester.isBlank()) {
            throw new IllegalArgumentException("Requester cannot be null or empty");
        }
    }
}
