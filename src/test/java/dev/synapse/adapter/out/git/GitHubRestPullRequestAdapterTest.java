package dev.synapse.adapter.out.git;

import dev.synapse.shared.config.GitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubRestPullRequestAdapterTest {

    @Test
    void createPullRequest_ShouldSendPostAndReturnHtmlUrl() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.github.com")
                .defaultHeaders(headers -> {
                    headers.setBearerAuth("test-token");
                    headers.setAccept(List.of(MediaType.parseMediaType("application/vnd.github+json")));
                });
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        GitHubRestPullRequestAdapter adapter = new GitHubRestPullRequestAdapter(
                new GitProperties("test-token", ""),
                builder.build()
        );

        server.expect(requestTo("https://api.github.com/repos/celikfatih/synapse/pulls"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(header("Accept", "application/vnd.github+json"))
                .andExpect(jsonPath("$.title").value("feat(123): autonomous PR"))
                .andExpect(jsonPath("$.head").value("feat/TASK-123"))
                .andExpect(jsonPath("$.base").value("main"))
                .andExpect(jsonPath("$.body").value("PR Description"))
                .andRespond(withSuccess("{\"html_url\": \"https://github.com/celikfatih/synapse/pull/42\"}", MediaType.APPLICATION_JSON));

        String prUrl = adapter.createPullRequest(
                "https://github.com/celikfatih/synapse.git",
                "feat/TASK-123",
                "feat(123): autonomous PR",
                "PR Description"
        );

        assertThat(prUrl).isEqualTo("https://github.com/celikfatih/synapse/pull/42");
        server.verify();
    }

    @Test
    void createPullRequest_ShouldParseVariousGitHubUrlFormats() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.github.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        GitHubRestPullRequestAdapter adapter = new GitHubRestPullRequestAdapter(
                new GitProperties("token", ""),
                builder.build()
        );

        server.expect(requestTo("https://api.github.com/repos/org/repo/pulls"))
                .andRespond(withSuccess("{\"html_url\": \"https://github.com/org/repo/pull/1\"}", MediaType.APPLICATION_JSON));

        String prUrl = adapter.createPullRequest(
                "https://github.com/org/repo",
                "feat/branch",
                "Title",
                "Body"
        );
        assertThat(prUrl).isEqualTo("https://github.com/org/repo/pull/1");
        server.verify();
    }

    @Test
    void createPullRequest_ShouldUseConfiguredBaseBranchFromGitProperties() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.github.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        GitHubRestPullRequestAdapter adapter = new GitHubRestPullRequestAdapter(
                new GitProperties("test-token", "develop"),
                builder.build()
        );

        server.expect(requestTo("https://api.github.com/repos/celikfatih/synapse/pulls"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.base").value("develop"))
                .andRespond(withSuccess("{\"html_url\": \"https://github.com/celikfatih/synapse/pull/100\"}", MediaType.APPLICATION_JSON));

        String prUrl = adapter.createPullRequest(
                "https://github.com/celikfatih/synapse.git",
                "feat/TASK-123",
                "feat(123): autonomous PR",
                "PR Description"
        );

        assertThat(prUrl).isEqualTo("https://github.com/celikfatih/synapse/pull/100");
        server.verify();
    }

    @Test
    void createPullRequest_ShouldReturnNullOrEmptyWhenGitHubApiFails() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.github.com");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        GitHubRestPullRequestAdapter adapter = new GitHubRestPullRequestAdapter(
                new GitProperties("token", ""),
                builder.build()
        );

        server.expect(requestTo("https://api.github.com/repos/celikfatih/synapse/pulls"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_CONTENT));

        String prUrl = adapter.createPullRequest(
                "https://github.com/celikfatih/synapse.git",
                "feat/TASK-123",
                "Title",
                "Body"
        );

        assertThat(prUrl).isEmpty();
        server.verify();
    }
}
