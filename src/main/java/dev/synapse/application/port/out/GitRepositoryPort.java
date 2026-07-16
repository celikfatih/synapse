package dev.synapse.application.port.out;

import java.nio.file.Path;

public interface GitRepositoryPort {
    /**
     * Clones a repository and checks out a specific branch.
     * @param repositoryUrl the URL of the repository to clone.
     * @param workspacePath the path where the repository will be cloned.
     * @param branchName the name of the branch to check out.
     */
    void cloneAndCheckout(String repositoryUrl, Path workspacePath, String branchName);

    /**
     * Commits and pushes changes to the remote repository.
     * @param workspacePath the path of the local repository.
     * @param commitMessage the message to be used for the commit.
     */
    void commitAndPush(Path workspacePath, String commitMessage);
}
