package dev.synapse.adapter.in.rest;

import dev.synapse.adapter.in.rest.request.ApiTaskSubmitRequest;
import dev.synapse.application.port.in.SubmitTaskUseCase;
import dev.synapse.application.port.in.command.SubmissionOrigin;
import dev.synapse.application.port.in.command.SubmitTaskCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rest/tasks")
public class ApiTaskRestAdapter {

    private final SubmitTaskUseCase submitTaskUseCase;

    @PostMapping("/submit")
    public ResponseEntity<Void> submit(@RequestBody ApiTaskSubmitRequest request,
                                       @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String userAgent) {
        SubmissionOrigin origin = SubmissionOrigin.restApi(userAgent);
        SubmitTaskCommand command = new SubmitTaskCommand(request.requester(),
                request.message(),
                origin);
        submitTaskUseCase.submit(command);
        return ResponseEntity.accepted().build();
    }
}
