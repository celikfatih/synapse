package dev.synapse.adapter.in.rest.mapper;

import dev.synapse.adapter.in.rest.request.SlackEventRequest;
import dev.synapse.application.port.in.command.SubmissionOrigin;
import dev.synapse.application.port.in.command.SubmitTaskCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class SlackPayloadTranslator {

    public Optional<SubmitTaskCommand> translateEvent(SlackEventRequest request) {
        if (!isValidEventCallback(request)) {
            return Optional.empty();
        }

        SlackEventRequest.SlackEventDetails event = request.event();
        if (isBotOrEchoMessage(event) || !isSupportedEventType(event)) {
            return Optional.empty();
        }

        return buildCommandFromEvent(event, request.teamId());
    }

    private boolean isValidEventCallback(SlackEventRequest request) {
        return "event_callback".equals(request.type()) && request.event() != null;
    }

    private boolean isBotOrEchoMessage(SlackEventRequest.SlackEventDetails event) {
        if (event.botId() != null || "bot_message".equals(event.subtype())) {
            log.debug("Ignoring Slack bot message or bot_id: {}", event.botId());
            return true;
        }
        return false;
    }

    private boolean isSupportedEventType(SlackEventRequest.SlackEventDetails event) {
        if ("app_mention".equals(event.type())) {
            return true;
        }
        return "message".equals(event.type()) && isDirectMessage(event);
    }

    private boolean isDirectMessage(SlackEventRequest.SlackEventDetails event) {
        return "im".equals(event.channelType()) || (event.channel() != null && event.channel().startsWith("D"));
    }

    private Optional<SubmitTaskCommand> buildCommandFromEvent(SlackEventRequest.SlackEventDetails event, String teamId) {
        if (event.user() == null) {
            return Optional.empty();
        }
        String cleanText = stripBotMention(event.text());
        if (cleanText.isBlank()) {
            return Optional.empty();
        }
        SubmissionOrigin origin = SubmissionOrigin.slack(teamId, event.channel());
        return Optional.of(new SubmitTaskCommand(event.user(), cleanText, origin));
    }

    public SubmitTaskCommand translateCommand(String text, String userId, String userName, String channelId,
                                              String teamId) {
        String requester = (userName != null && !userName.isBlank()) ? userName + " (" + userId + ")" : userId;
        SubmissionOrigin origin = SubmissionOrigin.slack(teamId, channelId);
        return new SubmitTaskCommand(requester, text, origin);
    }

    private String stripBotMention(String text) {
        if (text == null) return "";
        return text.replaceAll("<@[A-Z0-9]+>", "").trim();
    }
}
