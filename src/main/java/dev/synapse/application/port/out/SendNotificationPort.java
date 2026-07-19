package dev.synapse.application.port.out;

import dev.synapse.domain.notification.NotificationRequest;

public interface SendNotificationPort {
    /**
     * Sends a notification to a user.
     * @param request the notification request.
     */
    void sendNotification(NotificationRequest request);
}
