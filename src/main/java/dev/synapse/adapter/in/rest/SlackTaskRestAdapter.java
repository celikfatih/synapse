package dev.synapse.adapter.in.rest;

import dev.synapse.adapter.in.rest.mapper.SlackPayloadTranslator;
import dev.synapse.adapter.in.rest.request.SlackEventRequest;
import dev.synapse.adapter.in.rest.request.SlackWebhookRequest;
import dev.synapse.application.port.in.SubmitTaskUseCase;
import dev.synapse.application.port.in.command.SubmissionOrigin;
import dev.synapse.application.port.in.command.SubmitTaskCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/slack")
public class SlackTaskRestAdapter {

    private final SubmitTaskUseCase submitTaskUseCase;
    private final SlackPayloadTranslator payloadTranslator;

    @PostMapping(value = "/events")
    public ResponseEntity<String> handleEvents(
            @RequestBody SlackEventRequest request,
            @RequestHeader(value = "X-Slack-Retry-Num", required = false) String retryNum) {
        if ("url_verification".equals(request.type())) {
            log.info("Handling Slack URL verification challenge");
            return ResponseEntity.ok().body(request.challenge());
        }

        if (retryNum != null && !retryNum.isBlank()) {
            log.info("Ignoring Slack event delivery retry [X-Slack-Retry-Num={}] to prevent duplicate task submission", retryNum);
            return ResponseEntity.ok().build();
        }

        payloadTranslator.translateEvent(request).ifPresent(submitTaskUseCase::submit);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/commands", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Map<String, String>> handleCommands(
            @RequestParam("command") String command,
            @RequestParam("text") String text,
            @RequestParam("user_id") String userId,
            @RequestParam("user_name") String userName,
            @RequestParam("channel_id") String channelId,
            @RequestParam("team_id") String teamId
    ) {
        log.info("Received Slack slash command [{}] from user [{}] in team [{}]", command, userName, teamId);
        SubmitTaskCommand submitCommand = payloadTranslator.translateCommand(text, userId, userName, channelId, teamId);
        submitTaskUseCase.submit(submitCommand);

        return ResponseEntity.ok(Map.of(
                "response_type", "ephemeral",
                "text", "Task submitted to Synapse! 🚀"
        ));
    }

    @PostMapping("/tasks/submit")
    public ResponseEntity<Void> submitLegacy(@RequestBody SlackWebhookRequest request) {
        SubmissionOrigin origin = SubmissionOrigin.slack(request.teamId(), request.channelId());
        SubmitTaskCommand command = new SubmitTaskCommand(request.requester(), request.message(), origin);
        submitTaskUseCase.submit(command);
        return ResponseEntity.accepted().build();
    }
}
