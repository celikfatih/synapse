package dev.synapse.application.port.out;

import dev.synapse.domain.workspace.ProvisionedWorkspace;

public interface WorkspaceProvisioningPort {
    ProvisionedWorkspace provisionWorkspace(String taskId, String repositoryUrl);
}
