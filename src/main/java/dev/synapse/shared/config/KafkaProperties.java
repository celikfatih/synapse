package dev.synapse.shared.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "synapse.kafka")
public class KafkaProperties {
    private final String taskTopic;
    private final int maxAttempts;
    private final long backoffMs;

    public KafkaProperties(String taskTopic, Integer maxAttempts, Long backoffMs) {
        this.taskTopic = (taskTopic == null || taskTopic.isBlank()) ? "tasks" : taskTopic;
        this.maxAttempts = (maxAttempts == null || maxAttempts <= 0) ? 3 : maxAttempts;
        this.backoffMs = (backoffMs == null || backoffMs < 0) ? 1000L : backoffMs;
    }
}
