package dev.synapse.application.port.in.command;

import java.util.Map;

public record SubmissionOrigin(String channel, Map<String, String> metadata) {
    public SubmissionOrigin(String channel) {
        this(channel, Map.of());
    }

    public static SubmissionOrigin restApi(String userAgent) {
        return new SubmissionOrigin("REST_API", Map.of("userAgent", userAgent));
    }

    public static SubmissionOrigin slack(String teamId, String channelId) {
        return new SubmissionOrigin("SLACK", Map.of("teamId", teamId, "channelId", channelId));
    }
}
