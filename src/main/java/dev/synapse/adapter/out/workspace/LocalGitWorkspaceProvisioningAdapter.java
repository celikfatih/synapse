package dev.synapse.adapter.out.workspace;

import dev.synapse.application.port.out.WorkspaceProvisioningPort;
import dev.synapse.domain.workspace.ProvisionedWorkspace;
import dev.synapse.shared.config.WorkspaceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"local", "test"})
public class LocalGitWorkspaceProvisioningAdapter implements WorkspaceProvisioningPort {

    private final WorkspaceProperties workspaceProperties;

    @Override
    public ProvisionedWorkspace provisionWorkspace(String taskId, String repositoryUrl) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId cannot be null or blank");
        }

        String cleanTaskId = taskId.trim();
        String baseDir = workspaceProperties.getBaseDir();
        Path baseWorkspaceDir = Path.of(baseDir, cleanTaskId);

        try {
            Files.createDirectories(baseWorkspaceDir);
            log.info("Provisioned local workspace directory inside base dir [{}]: {}", baseDir, baseWorkspaceDir.toAbsolutePath());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to provision workspace directory in base dir: " + baseWorkspaceDir, e);
        }

        String branchName = "feat/" + cleanTaskId;
        return new ProvisionedWorkspace(cleanTaskId, repositoryUrl, baseWorkspaceDir.toAbsolutePath().toString(), branchName);
    }
}
