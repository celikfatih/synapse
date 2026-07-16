package dev.synapse.domain.task;

public record PullRequestUrl(String value) {
    public PullRequestUrl {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PullRequestUrl cannot be null or blank");
        }
    }
}
