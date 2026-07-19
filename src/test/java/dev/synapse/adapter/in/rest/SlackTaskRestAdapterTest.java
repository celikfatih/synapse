package dev.synapse.adapter.in.rest;

import dev.synapse.adapter.in.rest.mapper.SlackPayloadTranslator;
import dev.synapse.application.port.in.SubmitTaskUseCase;
import dev.synapse.application.port.in.command.SubmitTaskCommand;
import dev.synapse.shared.config.SlackProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SlackTaskRestAdapter.class)
@Import(SlackPayloadTranslator.class)
class SlackTaskRestAdapterTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubmitTaskUseCase submitTaskUseCase;

    @MockitoBean
    private SlackProperties slackProperties;

    @Test
    void handleEvents_ShouldReturnChallengeWhenTypeIsUrlVerification() throws Exception {
        String json = """
                {
                  "type": "url_verification",
                  "token": "verification_token",
                  "challenge": "challenge_token_abc123"
                }
                """;

        mockMvc.perform(post("/api/slack/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("challenge_token_abc123"));

        verify(submitTaskUseCase, never()).submit(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void handleEvents_ShouldSubmitTaskWhenAppMentionEvent() throws Exception {
        String json = """
                {
                  "type": "event_callback",
                  "team_id": "T12345",
                  "event": {
                    "type": "app_mention",
                    "user": "U98765",
                    "text": "<@U012BOT> fix the header styling on landing page",
                    "channel": "C54321"
                  }
                }
                """;

        mockMvc.perform(post("/api/slack/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        ArgumentCaptor<SubmitTaskCommand> captor = ArgumentCaptor.forClass(SubmitTaskCommand.class);
        verify(submitTaskUseCase).submit(captor.capture());
        SubmitTaskCommand command = captor.getValue();
        assertThat(command.requester()).isEqualTo("U98765");
        assertThat(command.message()).isEqualTo("fix the header styling on landing page");
        assertThat(command.origin().channel()).isEqualTo("SLACK");
        assertThat(command.origin().metadata())
                .containsEntry("teamId", "T12345")
                .containsEntry("channelId", "C54321");
    }

    @Test
    void handleEvents_ShouldIgnoreBotMessages() throws Exception {
        String json = """
                {
                  "type": "event_callback",
                  "team_id": "T12345",
                  "event": {
                    "type": "message",
                    "subtype": "bot_message",
                    "bot_id": "B11111",
                    "text": "I am a bot response",
                    "channel": "C54321"
                  }
                }
                """;

        mockMvc.perform(post("/api/slack/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(submitTaskUseCase, never()).submit(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void handleCommands_ShouldSubmitTaskAndReturnEphemeralResponse() throws Exception {
        mockMvc.perform(post("/api/slack/commands")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("command", "/synapse")
                        .param("text", "add unit tests for user service")
                        .param("user_id", "U1111")
                        .param("user_name", "alice")
                        .param("channel_id", "C2222")
                        .param("team_id", "T3333"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response_type").value("ephemeral"))
                .andExpect(jsonPath("$.text").value("Task submitted to Synapse! 🚀"));

        ArgumentCaptor<SubmitTaskCommand> captor = ArgumentCaptor.forClass(SubmitTaskCommand.class);
        verify(submitTaskUseCase).submit(captor.capture());
        SubmitTaskCommand command = captor.getValue();
        assertThat(command.requester()).isEqualTo("alice (U1111)");
        assertThat(command.message()).isEqualTo("add unit tests for user service");
        assertThat(command.origin().channel()).isEqualTo("SLACK");
        assertThat(command.origin().metadata())
                .containsEntry("teamId", "T3333")
                .containsEntry("channelId", "C2222");
    }

    @Test
    void submit_ShouldStillHandleExistingLegacyEndpoint() throws Exception {
        String json = """
                {
                  "requester": "bob",
                  "message": "legacy task submission",
                  "teamId": "T9999",
                  "channelId": "C8888"
                }
                """;

        mockMvc.perform(post("/api/slack/tasks/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isAccepted());

        ArgumentCaptor<SubmitTaskCommand> captor = ArgumentCaptor.forClass(SubmitTaskCommand.class);
        verify(submitTaskUseCase).submit(captor.capture());
        assertThat(captor.getValue().requester()).isEqualTo("bob");
        assertThat(captor.getValue().message()).isEqualTo("legacy task submission");
    }

    @Test
    void handleEvents_ShouldIgnoreMessageEventInRegularChannelToPreventDuplicates() throws Exception {
        String json = """
                {
                  "type": "event_callback",
                  "team_id": "T12345",
                  "event": {
                    "type": "message",
                    "user": "U98765",
                    "text": "<@U012BOT> fix the header styling on landing page",
                    "channel": "C54321",
                    "channel_type": "channel"
                  }
                }
                """;

        mockMvc.perform(post("/api/slack/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(submitTaskUseCase, never()).submit(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void handleEvents_ShouldProcessMessageEventInDirectMessage() throws Exception {
        String json = """
                {
                  "type": "event_callback",
                  "team_id": "T12345",
                  "event": {
                    "type": "message",
                    "user": "U98765",
                    "text": "fix direct message issue",
                    "channel": "D12345",
                    "channel_type": "im"
                  }
                }
                """;

        mockMvc.perform(post("/api/slack/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        ArgumentCaptor<SubmitTaskCommand> captor = ArgumentCaptor.forClass(SubmitTaskCommand.class);
        verify(submitTaskUseCase).submit(captor.capture());
        assertThat(captor.getValue().requester()).isEqualTo("U98765");
        assertThat(captor.getValue().message()).isEqualTo("fix direct message issue");
    }

    @Test
    void handleEvents_ShouldIgnoreSlackRetryHeaderToPreventDuplicates() throws Exception {
        String json = """
                {
                  "type": "event_callback",
                  "team_id": "T12345",
                  "event": {
                    "type": "app_mention",
                    "user": "U98765",
                    "text": "<@U012BOT> fix something",
                    "channel": "C54321"
                  }
                }
                """;

        mockMvc.perform(post("/api/slack/events")
                        .header("X-Slack-Retry-Num", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        verify(submitTaskUseCase, never()).submit(org.mockito.ArgumentMatchers.any());
    }
}
