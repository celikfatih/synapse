package dev.synapse.adapter.out.jira;

import dev.synapse.adapter.out.jira.model.JiraFields;
import dev.synapse.adapter.out.jira.model.JiraIssue;
import dev.synapse.adapter.out.jira.model.adf.AdfDocument;
import dev.synapse.adapter.out.jira.parser.AdfParser;
import dev.synapse.domain.task.EnrichedTaskContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JiraRestAdapterTest {

    @Mock
    private RestClient jiraRestClient;

    @Mock
    private AdfParser adfParser;

    @InjectMocks
    private JiraRestAdapter jiraRestAdapter;

    @Test
    @SuppressWarnings("unchecked")
    void enrichTask_ShouldReturnEnrichedTaskContext_WhenTicketKeyIsValid() {
        // given
        String ticketKey = "PAY-1042";
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        JiraFields fields = new JiraFields("Fix refund bug", new AdfDocument("doc", "text", List.of()));
        JiraIssue issue = new JiraIssue("1001", "PAY-1042", fields);

        when(jiraRestClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(eq("/rest/api/3/issue/{key}"), eq(ticketKey))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(JiraIssue.class)).thenReturn(issue);
        when(adfParser.parse(any())).thenReturn("Parsed description");

        // when
        EnrichedTaskContext result = jiraRestAdapter.enrichTask(ticketKey);

        // then
        assertThat(result).isNotNull();
        assertThat(result.ticketKey()).isEqualTo("PAY-1042");
        assertThat(result.summary()).isEqualTo("Fix refund bug");
        assertThat(result.description()).isEqualTo("Parsed description");
        assertThat(result.acceptanceCriteria()).isInstanceOf(List.class);
    }

    @Test
    void enrichTask_ShouldThrowIllegalArgumentException_WhenTicketKeyIsBlankOrNull() {
        assertThatThrownBy(() -> jiraRestAdapter.enrichTask("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ticketKey cannot be null or blank");

        assertThatThrownBy(() -> jiraRestAdapter.enrichTask(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ticketKey cannot be null or blank");
    }
}
