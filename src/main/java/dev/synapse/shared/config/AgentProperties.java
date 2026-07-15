package dev.synapse.shared.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "synapse.agent.docker")
public class AgentProperties {
    private final String image;
    private final String containerWorkspacePath;
    private final String memoryLimit;
    private final String cpus;
    private final long timeoutSeconds;
    private final String model;
    private final String apiBaseUrl;

    public AgentProperties(String image, String containerWorkspacePath, String memoryLimit, String cpus,
                           Long timeoutSeconds, String model, String apiBaseUrl) {
        this.image = (image == null || image.isBlank()) ? "synapse-sandbox:java25" : image;
        this.containerWorkspacePath = (containerWorkspacePath == null || containerWorkspacePath.isBlank()) ? "/workspace" : containerWorkspacePath;
        this.memoryLimit = (memoryLimit == null || memoryLimit.isBlank()) ? "8g" : memoryLimit;
        this.cpus = (cpus == null || cpus.isBlank()) ? "4" : cpus;
        this.timeoutSeconds = (timeoutSeconds == null || timeoutSeconds <= 0) ? 1800L : timeoutSeconds;
        this.model = (model == null || model.isBlank()) ? "qwen2.5-coder:7b" : model;
        this.apiBaseUrl = (apiBaseUrl == null || apiBaseUrl.isBlank()) ? "http://localhost:8080" : apiBaseUrl;
    }
}
