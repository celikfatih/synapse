package dev.synapse.shared.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "synapse.git")
public class GitProperties {
    private final String token;
    private final String defaultBaseBranch;

    public GitProperties(String token, String defaultBaseBranch) {
        this.token = token;
        this.defaultBaseBranch = defaultBaseBranch != null && !defaultBaseBranch.isBlank() ? defaultBaseBranch : "main";
    }
}
