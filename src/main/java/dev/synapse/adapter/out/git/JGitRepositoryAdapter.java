package dev.synapse.adapter.out.git;

import dev.synapse.application.port.out.GitRepositoryPort;
import dev.synapse.shared.config.GitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LsRemoteCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

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

    @Override
    public void commitAndPush(Path workspacePath, String commitMessage) {
        try (Git git = Git.open(workspacePath.toFile())) {
            log.info("Staging changes in workspace [{}]", workspacePath);
            git.add().addFilepattern(".").call();

            log.info("Committing changes with message: {}", commitMessage);
            git.commit().setMessage(commitMessage).call();

            log.info("Pushing feature branch from [{}] to remote origin", workspacePath);
            PushCommand pushCommand = git.push();
            if (gitProperties != null && gitProperties.getToken() != null && !gitProperties.getToken().isBlank()) {
                log.debug("Authenticating push with GitHub organization via token");
                pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                        gitProperties.getToken(),
                        ""
                ));
            }
            pushCommand.call();
            log.info("Successfully pushed branch to remote origin for workspace [{}]", workspacePath);
        } catch (GitAPIException e) {
            log.warn("Git API error during commit and push in [{}]: {}", workspacePath, e.getMessage());
        } catch (Exception e) {
            log.warn("Error committing and pushing repository in [{}]: {}", workspacePath, e.getMessage());
        }
    }

    @Override
    public boolean validateRepositoryExists(String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            return false;
        }
        try {
            log.info("Validating accessibility and existence of remote repository: [{}]", repositoryUrl);
            LsRemoteCommand lsRemoteCommand = Git.lsRemoteRepository()
                    .setRemote(repositoryUrl);

            if (gitProperties != null && gitProperties.getToken() != null && !gitProperties.getToken().isBlank()) {
                lsRemoteCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                        gitProperties.getToken(),
                        ""
                ));
            }

            Collection<Ref> refs = lsRemoteCommand.call();
            boolean valid = refs != null && !refs.isEmpty();
            if (valid) {
                log.info("Remote repository [{}] validated successfully", repositoryUrl);
            } else {
                log.warn("Remote repository [{}] returned empty references or could not be validated", repositoryUrl);
            }
            return valid;
        } catch (GitAPIException e) {
            log.warn("Validation failed for repository [{}]: {}", repositoryUrl, e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Unexpected error validating repository [{}]: {}", repositoryUrl, e.getMessage());
            return false;
        }
    }
}
