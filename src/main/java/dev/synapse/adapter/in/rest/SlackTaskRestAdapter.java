package dev.synapse.adapter.in.rest;

import dev.synapse.adapter.in.rest.request.SlackWebhookRequest;
import dev.synapse.application.port.in.SubmitTaskUseCase;
import dev.synapse.application.port.in.command.SubmissionOrigin;
import dev.synapse.application.port.in.command.SubmitTaskCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/slack/tasks")
public class SlackTaskRestAdapter {

    private final SubmitTaskUseCase submitTaskUseCase;

    @PostMapping("/submit")
    public ResponseEntity<Void> submit(@RequestBody SlackWebhookRequest request) {
        SubmissionOrigin origin = SubmissionOrigin.slack(request.teamId(), request.channelId());
        SubmitTaskCommand command = new SubmitTaskCommand(request.requester(),
                request.message(),
                origin);
        submitTaskUseCase.submit(command);
        return ResponseEntity.accepted().build();
    }
}
