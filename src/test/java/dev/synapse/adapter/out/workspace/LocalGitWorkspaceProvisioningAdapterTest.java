package dev.synapse.adapter.out.workspace;

import dev.synapse.domain.workspace.ProvisionedWorkspace;
import dev.synapse.shared.config.WorkspaceProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalGitWorkspaceProvisioningAdapterTest {

    private LocalGitWorkspaceProvisioningAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LocalGitWorkspaceProvisioningAdapter(new WorkspaceProperties(null));
    }

    @Test
    void provisionWorkspace_ShouldCreateDirectoryInsideSystemTempFolder_WhenTaskIdIsValid() {
        // given
        String taskId = "TASK-999";
        String repoUrl = "git@github.com:myorg/checkout-service.git";

        // when
        ProvisionedWorkspace result = adapter.provisionWorkspace(taskId, repoUrl);

        // then
        assertThat(result).isNotNull();
        assertThat(result.taskId()).isEqualTo(taskId);
        assertThat(result.repositoryUrl()).isEqualTo(repoUrl);
        assertThat(result.branchName()).isEqualTo("feat/TASK-999");
        assertThat(result.workspacePath()).contains(System.getProperty("java.io.tmpdir"));
        assertThat(Files.exists(Path.of(result.workspacePath()))).isTrue();
    }

    @Test
    void provisionWorkspace_ShouldHandleNullRepositoryUrlGracefully_InSystemTempFolder() {
        // given
        String taskId = "TASK-888";

        // when
        ProvisionedWorkspace result = adapter.provisionWorkspace(taskId, null);

        // then
        assertThat(result).isNotNull();
        assertThat(result.taskId()).isEqualTo(taskId);
        assertThat(result.repositoryUrl()).isNull();
        assertThat(result.branchName()).isEqualTo("feat/TASK-888");
        assertThat(result.workspacePath()).contains(System.getProperty("java.io.tmpdir"));
        assertThat(Files.exists(Path.of(result.workspacePath()))).isTrue();
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
