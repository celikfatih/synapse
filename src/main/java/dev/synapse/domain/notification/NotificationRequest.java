package dev.synapse.domain.notification;

public record NotificationRequest(
        String taskId,
        String correlationId,
        String requester,
        String source,
        boolean success,
        String summary,
        String pullRequestUrl,
        String errorMessage
) {
    public static NotificationRequest success(String taskId, String correlationId, String requester, String source,
                                              String summary, String pullRequestUrl) {
        return new NotificationRequest(taskId, correlationId, requester, source, true, summary, pullRequestUrl, null);
    }

    public static NotificationRequest failure(String taskId, String correlationId, String requester,
                                              String source, String errorMessage) {
        return new NotificationRequest(taskId, correlationId, requester, source, false, null, null, errorMessage);
    }
}
