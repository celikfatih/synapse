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
@Profile({"prod", "default"})
public class DockerWorkspaceProvisioningAdapter implements WorkspaceProvisioningPort {

    private final WorkspaceProperties workspaceProperties;

    @Override
    public ProvisionedWorkspace provisionWorkspace(String taskId, String repositoryUrl) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId cannot be null or blank");
        }

        String cleanTaskId = taskId.trim();
        String baseDir = workspaceProperties.getBaseDir();
        // Host volume directory where the Git repository is cloned before mounting into Docker sandbox at /workspace
        Path hostMountPath = Path.of(baseDir, cleanTaskId, "workspace");

        try {
            Files.createDirectories(hostMountPath);
            log.info("Provisioned host workspace volume at [{}] to be mounted into Docker sandbox at /workspace for taskId [{}]",
                    hostMountPath.toAbsolutePath(), cleanTaskId);
        } catch (IOException e) {
            log.warn("Could not create physical directory at [{}], continuing with path reference: {}",
                    hostMountPath, e.getMessage());
        }

        String branchName = "feat/" + cleanTaskId;
        return new ProvisionedWorkspace(cleanTaskId, repositoryUrl, hostMountPath.toAbsolutePath().toString(), branchName);
    }
}
