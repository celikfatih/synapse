package dev.synapse.shared.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@ConfigurationProperties(prefix = "synapse.workspace")
public class WorkspaceProperties {
    private final String baseDir;

    public WorkspaceProperties(String baseDir) {
        this.baseDir = (baseDir == null || baseDir.isBlank())
                ? System.getProperty("java.io.tmpdir") + "/synapse-workspaces"
                : baseDir;
    }
}
