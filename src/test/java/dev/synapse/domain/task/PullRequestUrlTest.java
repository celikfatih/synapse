package dev.synapse.domain.task;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PullRequestUrlTest {

    @Test
    void constructor_ShouldCreateValidPullRequestUrl() {
        PullRequestUrl prUrl = new PullRequestUrl("https://github.com/celikfatih/synapse/pull/1");
        assertThat(prUrl.value()).isEqualTo("https://github.com/celikfatih/synapse/pull/1");
    }

    @Test
    void constructor_ShouldThrowException_WhenValueIsNull() {
        assertThatThrownBy(() -> new PullRequestUrl(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PullRequestUrl cannot be null or blank");
    }

    @Test
    void constructor_ShouldThrowException_WhenValueIsBlank() {
        assertThatThrownBy(() -> new PullRequestUrl("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PullRequestUrl cannot be null or blank");
    }
}
