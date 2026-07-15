package dev.synapse.shared.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "synapse.jira")
public class JiraProperties {
    private final String baseUrl;
    private final String username;
    private final String token;
    private final String project;
}
