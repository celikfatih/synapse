package dev.synapse.adapter.out.notification;

import dev.synapse.domain.notification.NotificationRequest;
import dev.synapse.shared.config.SlackProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SlackWebhookNotificationAdapterTest {

    @Test
    void sendNotification_ShouldSendFormattedSlackBlockKitMessageWhenSuccess() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        SlackProperties properties = new SlackProperties("https://hooks.slack.com/services/T00/B00/test", "secret", "token");
        SlackWebhookNotificationAdapter adapter = new SlackWebhookNotificationAdapter(properties, builder.build(), new dev.synapse.adapter.out.notification.payload.SlackBlockKitFactory());

        server.expect(requestTo("https://hooks.slack.com/services/T00/B00/test"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.blocks[0].text.type").value("mrkdwn"))
                .andExpect(jsonPath("$.blocks[0].text.text").value("✅ *Synapse Task Completed Successfully*"))
                .andExpect(jsonPath("$.blocks[1].fields[0].text").value("*Task ID:*\nTASK-123"))
                .andExpect(jsonPath("$.blocks[1].fields[1].text").value("*Correlation ID:*\nCORR-123"))
                .andExpect(jsonPath("$.blocks[1].fields[2].text").value("*Requester:*\nFATIH"))
                .andExpect(jsonPath("$.blocks[2].text.text").value("*Pull Request:*\n<https://github.com/pr/1|View Pull Request on GitHub>"))
                .andRespond(withSuccess());

        NotificationRequest request = NotificationRequest.success("TASK-123", "CORR-123", "FATIH", "SLACK", "Completed task", "https://github.com/pr/1");
        adapter.sendNotification(request);

        server.verify();
    }

    @Test
    void sendNotification_ShouldSendFailureMessageWhenFailed() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        SlackProperties properties = new SlackProperties("https://hooks.slack.com/services/T00/B00/test", "secret", "");
        SlackWebhookNotificationAdapter adapter = new SlackWebhookNotificationAdapter(properties, builder.build(), new dev.synapse.adapter.out.notification.payload.SlackBlockKitFactory());

        server.expect(requestTo("https://hooks.slack.com/services/T00/B00/test"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.blocks[0].text.type").value("mrkdwn"))
                .andExpect(jsonPath("$.blocks[0].text.text").value("❌ *Synapse Task Execution Failed*"))
                .andExpect(jsonPath("$.blocks[1].fields[0].text").value("*Task ID:*\nTASK-999"))
                .andExpect(jsonPath("$.blocks[1].fields[1].text").value("*Correlation ID:*\nCORR-999"))
                .andExpect(jsonPath("$.blocks[1].fields[2].text").value("*Requester:*\nFATIH"))
                .andExpect(jsonPath("$.blocks[2].text.text").value("*Error Details:*\n```Agent execution timeout```"))
                .andRespond(withSuccess());

        NotificationRequest request = NotificationRequest.failure("TASK-999", "CORR-999", "FATIH", "SLACK", "Agent execution timeout");
        adapter.sendNotification(request);

        server.verify();
    }

    @Test
    void sendNotification_ShouldSkipWhenWebhookUrlIsMissingOrBlank() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        SlackProperties properties = new SlackProperties("   ", "secret", "");
        SlackWebhookNotificationAdapter adapter = new SlackWebhookNotificationAdapter(properties, builder.build(), new dev.synapse.adapter.out.notification.payload.SlackBlockKitFactory());

        NotificationRequest request = NotificationRequest.success("TASK-123", "CORR-123", "FATIH", "SLACK", "Completed", "url");
        adapter.sendNotification(request);

        server.verify();
    }
}
