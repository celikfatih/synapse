package dev.synapse.adapter.out.notification;

import dev.synapse.adapter.out.notification.payload.SlackBlockKitFactory;
import dev.synapse.application.port.out.SendNotificationPort;
import dev.synapse.domain.notification.NotificationRequest;
import dev.synapse.shared.config.SlackProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackWebhookNotificationAdapter implements SendNotificationPort {

    private final SlackProperties slackProperties;
    private final RestClient slackRestClient;
    private final SlackBlockKitFactory blockKitFactory;

    @Override
    public void sendNotification(NotificationRequest request) {
        String webhookUrl = slackProperties != null ? slackProperties.getWebhookUrl() : null;
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.info("Skipping Slack notification for task [{}] (webhook URL not configured)", request.taskId());
            return;
        }

        Map<String, Object> payload = blockKitFactory.createPayload(request);

        try {
            slackRestClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Successfully sent Slack notification for task [{}]", request.taskId());
        } catch (Exception e) {
            log.error("Failed to send Slack notification for task [{}]: {}", request.taskId(), e.getMessage(), e);
        }
    }
}
