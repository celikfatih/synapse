package dev.synapse.application.port.out;

public interface CreatePullRequestPort {
    /**
     * Creates a pull request in a repository.
     * @param repositoryUrl the URL of the repository.
     * @param branchName branch name to create the pull request from.
     * @param title the title of the pull request.
     * @param body the body of the pull request.
     * @return the URL of the created pull request.
     */
    String createPullRequest(String repositoryUrl, String branchName, String title, String body);
}
