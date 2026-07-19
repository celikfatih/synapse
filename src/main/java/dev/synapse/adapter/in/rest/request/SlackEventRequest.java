package dev.synapse.adapter.in.rest.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SlackEventRequest(
        String type,
        String token,
        String challenge,
        @JsonProperty("team_id") String teamId,
        SlackEventDetails event
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SlackEventDetails(
            String type,
            String subtype,
            String user,
            String text,
            String channel,
            @JsonProperty("bot_id") String botId,
            @JsonProperty("channel_type") String channelType
    ) {}
}
