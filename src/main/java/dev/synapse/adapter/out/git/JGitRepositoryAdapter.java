package dev.synapse.adapter.out.git;

import dev.synapse.application.port.out.GitRepositoryPort;
import dev.synapse.shared.config.GitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class JGitRepositoryAdapter implements GitRepositoryPort {

    private final GitProperties gitProperties;

    @Override
    public void cloneAndCheckout(String repositoryUrl, Path workspacePath, String branchName) {
        try {
            Files.createDirectories(workspacePath);
            if (repositoryUrl != null && !repositoryUrl.isBlank()) {
                log.info("Cloning Git repository [{}] into [{}]", repositoryUrl, workspacePath);
                CloneCommand cloneCommand = Git.cloneRepository()
                        .setURI(repositoryUrl)
                        .setDirectory(workspacePath.toFile());

                if (gitProperties != null && gitProperties.getToken() != null && !gitProperties.getToken().isBlank()) {
                    log.debug("Authenticating with GitHub organization via token for user");
                    cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                            gitProperties.getToken(),
                            ""
                    ));
                }

                try (Git git = cloneCommand.call()) {
                    git.checkout()
                            .setCreateBranch(true)
                            .setName(branchName)
                            .call();
                    log.info("Checked out new feature branch [{}] in repository [{}]", branchName, workspacePath);
                }
            }
        } catch (GitAPIException e) {
            log.warn("Could not clone remote repository [{}]: {}, continuing with directory reference",
                    repositoryUrl, e.getMessage());
        } catch (Exception e) {
            log.warn("Error cloning repository into [{}]: {}", workspacePath, e.getMessage());
        }
    }
}
