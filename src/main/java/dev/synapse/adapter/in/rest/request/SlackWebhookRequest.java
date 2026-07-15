package dev.synapse.adapter.in.rest.request;

public record SlackWebhookRequest(String requester, String message, String teamId, String channelId) {
}
