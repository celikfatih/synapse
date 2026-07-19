package dev.synapse.shared.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "synapse.slack")
public class SlackProperties {
    private final String webhookUrl;
    private final String signingSecret;
    private final String botToken;
}
