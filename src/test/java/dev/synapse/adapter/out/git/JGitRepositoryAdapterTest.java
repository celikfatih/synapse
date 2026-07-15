package dev.synapse.adapter.out.git;

import dev.synapse.shared.config.GitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JGitRepositoryAdapterTest {

    private JGitRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JGitRepositoryAdapter(new GitProperties(""));
    }

    @Test
    void cloneAndCheckout_ShouldCloneRepositoryAndCheckoutFeatureBranch(@TempDir Path tempOriginDir, @TempDir Path tempTargetDir) throws Exception {
        // given: create a real local Git repository to act as origin
        try (org.eclipse.jgit.api.Git git = org.eclipse.jgit.api.Git.init().setDirectory(tempOriginDir.toFile()).call()) {
            File readme = new File(tempOriginDir.toFile(), "README.md");
            Files.writeString(readme.toPath(), "# Sample Git Project");
            git.add().addFilepattern("README.md").call();
            git.commit().setMessage("Initial commit").call();
        }

        String originRepoUri = tempOriginDir.toAbsolutePath().toUri().toString();
        Path targetWorkspaceDir = tempTargetDir.resolve("workspace");

        // when
        adapter.cloneAndCheckout(originRepoUri, targetWorkspaceDir, "feat/TASK-GIT-1");

        // then
        assertThat(Files.exists(targetWorkspaceDir.resolve("README.md"))).isTrue();
        assertThat(Files.readString(targetWorkspaceDir.resolve("README.md"))).isEqualTo("# Sample Git Project");

        try (org.eclipse.jgit.api.Git clonedGit = org.eclipse.jgit.api.Git.open(targetWorkspaceDir.toFile())) {
            assertThat(clonedGit.getRepository().getBranch()).isEqualTo("feat/TASK-GIT-1");
        }
    }

    @Test
    void cloneAndCheckout_ShouldSucceedWhenGitPropertiesHasToken(@TempDir Path tempOriginDir, @TempDir Path tempTargetDir) throws Exception {
        // given
        JGitRepositoryAdapter authAdapter = new JGitRepositoryAdapter(
                new GitProperties("dummy-secret-token")
        );
        try (org.eclipse.jgit.api.Git git = org.eclipse.jgit.api.Git.init().setDirectory(tempOriginDir.toFile()).call()) {
            File readme = new File(tempOriginDir.toFile(), "README.md");
            Files.writeString(readme.toPath(), "# Token Authenticated Project");
            git.add().addFilepattern("README.md").call();
            git.commit().setMessage("Initial commit").call();
        }

        String originRepoUri = tempOriginDir.toAbsolutePath().toUri().toString();
        Path targetWorkspaceDir = tempTargetDir.resolve("auth-workspace");

        // when
        authAdapter.cloneAndCheckout(originRepoUri, targetWorkspaceDir, "feat/TASK-AUTH-1");

        // then
        assertThat(Files.exists(targetWorkspaceDir.resolve("README.md"))).isTrue();
        assertThat(Files.readString(targetWorkspaceDir.resolve("README.md"))).isEqualTo("# Token Authenticated Project");
    }
}
