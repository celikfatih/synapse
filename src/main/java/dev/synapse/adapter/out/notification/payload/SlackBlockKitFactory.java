package dev.synapse.adapter.out.notification.payload;

import dev.synapse.domain.notification.NotificationRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SlackBlockKitFactory {

    public Map<String, Object> createPayload(NotificationRequest request) {
        List<Map<String, Object>> blocks = new ArrayList<>();

        if (request.success()) {
            blocks.add(section("✅ *Synapse Task Completed Successfully*"));
            blocks.add(fieldsSection(request.taskId(), request.correlationId(), request.requester()));
            addSectionIfPresent(blocks, request.pullRequestUrl(), "*Pull Request:*\n<" + request.pullRequestUrl() + "|View Pull Request on GitHub>");
            addSectionIfPresent(blocks, request.summary(), "*Summary:*\n" + request.summary());
        } else {
            blocks.add(section("❌ *Synapse Task Execution Failed*"));
            blocks.add(fieldsSection(request.taskId(), request.correlationId(), request.requester()));
            addSectionIfPresent(blocks, request.errorMessage(), "*Error Details:*\n```" + request.errorMessage() + "```");
        }

        return Map.of("blocks", blocks);
    }

    private Map<String, Object> section(String markdownText) {
        return Map.of("type", "section", "text", markdown(truncate(markdownText, 2900)));
    }

    private Map<String, Object> fieldsSection(String taskId, String correlationId, String requester) {
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(markdown("*Task ID:*\n" + (taskId != null ? truncate(taskId, 1900) : "N/A")));
        fields.add(markdown("*Correlation ID:*\n" + (correlationId != null ? truncate(correlationId, 1900) : "N/A")));
        if (requester != null) {
            fields.add(markdown("*Requester:*\n" + truncate(requester, 1900)));
        }
        return Map.of("type", "section", "fields", fields);
    }

    private void addSectionIfPresent(List<Map<String, Object>> blocks, String content, String formattedMarkdown) {
        if (content != null && !content.isBlank()) {
            blocks.add(section(formattedMarkdown));
        }
    }

    private Map<String, Object> markdown(String text) {
        return Map.of("type", "mrkdwn", "text", text != null ? text : "");
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "\n... [truncated]";
    }
}
