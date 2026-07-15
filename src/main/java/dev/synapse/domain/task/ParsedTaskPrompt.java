package dev.synapse.domain.task;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record ParsedTaskPrompt(
        String ticketKey,
        Optional<String> repositoryUrl,
        String cleanPrompt
) {
    public ParsedTaskPrompt {
        Objects.requireNonNull(ticketKey, "ticketKey cannot be null");
        Objects.requireNonNull(repositoryUrl, "repositoryUrl cannot be null");
        Objects.requireNonNull(cleanPrompt, "cleanPrompt cannot be null");
    }

    private static final Pattern REPO_FLAG_PATTERN = Pattern.compile("--repo(?:=|\\s+)(\\S+)");
    private static final Pattern TICKET_KEY_PATTERN = Pattern.compile("[A-Z]{2,10}-\\d+");

    public static ParsedTaskPrompt parse(String rawPrompt) {
        if (rawPrompt == null || rawPrompt.isBlank()) {
            throw new IllegalArgumentException("rawPrompt cannot be null or blank");
        }

        String prompt = rawPrompt.trim();

        Optional<String> repositoryUrl = Optional.empty();
        Matcher repoMatcher = REPO_FLAG_PATTERN.matcher(prompt);
        if (repoMatcher.find()) {
            repositoryUrl = Optional.of(repoMatcher.group(1));
            prompt = repoMatcher.replaceAll("").trim();
        }

        String ticketKey = prompt;
        Matcher ticketMatcher = TICKET_KEY_PATTERN.matcher(prompt);
        if (ticketMatcher.find()) {
            ticketKey = ticketMatcher.group();
            prompt = prompt.replaceFirst(Pattern.quote(ticketKey), "").trim();
        }

        return new ParsedTaskPrompt(ticketKey, repositoryUrl, prompt);
    }
}
