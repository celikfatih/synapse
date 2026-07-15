package dev.synapse.domain.workspace;

import java.util.Objects;

public record ProvisionedWorkspace(
        String taskId,
        String repositoryUrl,
        String workspacePath,
        String branchName
) {
    public ProvisionedWorkspace {
        Objects.requireNonNull(taskId, "taskId cannot be null");
        Objects.requireNonNull(workspacePath, "workspacePath cannot be null");
        Objects.requireNonNull(branchName, "branchName cannot be null");
    }
}
