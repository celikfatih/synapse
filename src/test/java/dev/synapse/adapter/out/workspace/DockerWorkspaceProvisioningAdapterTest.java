package dev.synapse.adapter.out.workspace;

import dev.synapse.domain.workspace.ProvisionedWorkspace;
import dev.synapse.shared.config.WorkspaceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DockerWorkspaceProvisioningAdapterTest {

    private DockerWorkspaceProvisioningAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DockerWorkspaceProvisioningAdapter(new WorkspaceProperties(null));
    }

    @Test
    void provisionWorkspace_ShouldReturnIsolatedContainerWorkspacePath_WhenTaskIdIsValid() {
        // given
        String taskId = "TASK-500";
        String repositoryUrl = "git@github.com:myorg/service.git";

        // when
        ProvisionedWorkspace result = adapter.provisionWorkspace(taskId, repositoryUrl);

        // then
        assertThat(result).isNotNull();
        assertThat(result.taskId()).isEqualTo(taskId);
        assertThat(result.repositoryUrl()).isEqualTo(repositoryUrl);
        assertThat(result.branchName()).isEqualTo("feat/TASK-500");
        String expectedPath = Path.of(System.getProperty("java.io.tmpdir"), "synapse-workspaces", "TASK-500", "workspace").toAbsolutePath().toString();
        assertThat(result.workspacePath()).isEqualTo(expectedPath);
    }

    @Test
    void provisionWorkspace_ShouldThrowIllegalArgumentException_WhenTaskIdIsBlankOrNull() {
        assertThatThrownBy(() -> adapter.provisionWorkspace("   ", "url"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taskId cannot be null or blank");

        assertThatThrownBy(() -> adapter.provisionWorkspace(null, "url"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taskId cannot be null or blank");
    }
}
