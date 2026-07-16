package dev.synapse.adapter.out.git;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.synapse.application.port.out.CreatePullRequestPort;
import dev.synapse.shared.config.GitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitHubRestPullRequestAdapter implements CreatePullRequestPort {

    private final GitProperties gitProperties;
    private final RestClient gitRestClient;

    record PullRequestRequest(String title, String body, String head, String base) {}
    record PullRequestResponse(@JsonProperty("html_url") String htmlUrl) {}

    @Override
    public String createPullRequest(String repositoryUrl, String branchName, String title, String body) {
        if (repositoryUrl == null || repositoryUrl.isBlank() || branchName == null || branchName.isBlank()) {
            log.warn("Cannot create PR due to missing repositoryUrl or branchName");
            return "";
        }

        try {
            String[] ownerAndRepo = parseOwnerAndRepo(repositoryUrl);
            String owner = ownerAndRepo[0];
            String repo = ownerAndRepo[1];

            String baseBranch = (gitProperties != null && gitProperties.getDefaultBaseBranch() != null && !gitProperties.getDefaultBaseBranch().isBlank())
                    ? gitProperties.getDefaultBaseBranch()
                    : "main";
            PullRequestRequest payload = new PullRequestRequest(title, body, branchName, baseBranch);
            log.info("Creating GitHub Pull Request for [{}/{}] head [{}] -> base [{}]", owner, repo, branchName, baseBranch);

            PullRequestResponse response = gitRestClient.post()
                    .uri("/repos/{owner}/{repo}/pulls", owner, repo)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(PullRequestResponse.class);

            if (response != null && response.htmlUrl() != null) {
                log.info("Successfully created GitHub Pull Request: {}", response.htmlUrl());
                return response.htmlUrl();
            }
        } catch (Exception e) {
            log.warn("Failed to create GitHub Pull Request for repository [{}], branch [{}]: {}",
                    repositoryUrl, branchName, e.getMessage());
        }
        return "";
    }

    private String[] parseOwnerAndRepo(String repositoryUrl) {
        String cleanUrl = repositoryUrl.trim();
        if (cleanUrl.endsWith(".git")) {
            cleanUrl = cleanUrl.substring(0, cleanUrl.length() - 4);
        }

        int githubIdx = cleanUrl.indexOf("github.com/");
        if (githubIdx == -1) {
            githubIdx = cleanUrl.indexOf("github.com:");
        }
        if (githubIdx != -1) {
            cleanUrl = cleanUrl.substring(githubIdx + 11);
        }

        while (cleanUrl.startsWith("/") || cleanUrl.startsWith(":")) {
            cleanUrl = cleanUrl.substring(1);
        }

        String[] parts = cleanUrl.split("/");
        if (parts.length >= 2 && !parts[0].isBlank() && !parts[1].isBlank()) {
            return new String[]{parts[0], parts[1]};
        }
        throw new IllegalArgumentException("Cannot parse owner and repo from repository URL: " + repositoryUrl);
    }
}
