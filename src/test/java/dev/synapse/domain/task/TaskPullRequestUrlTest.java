package dev.synapse.domain.task;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskPullRequestUrlTest {

    @Test
    void attachPullRequestUrl_ShouldSetPullRequestUrlOnTask() {
        Task task = new Task("dev", "Fix bug", "corr-1", "SLACK");
        assertThat(task.getPullRequestUrl()).isNull();

        String prUrl = "https://github.com/celikfatih/synapse/pull/42";
        task.attachPullRequestUrl(prUrl);

        assertThat(task.getPullRequestUrl()).isEqualTo(new PullRequestUrl(prUrl));

        PullRequestUrl domainPrUrl = new PullRequestUrl("https://github.com/celikfatih/synapse/pull/99");
        task.attachPullRequestUrl(domainPrUrl);
        assertThat(task.getPullRequestUrl()).isEqualTo(domainPrUrl);
    }
}
