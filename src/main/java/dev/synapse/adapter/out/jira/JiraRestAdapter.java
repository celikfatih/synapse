package dev.synapse.adapter.out.jira;

import dev.synapse.adapter.out.jira.model.JiraIssue;
import dev.synapse.adapter.out.jira.parser.AdfParser;
import dev.synapse.application.port.out.JiraEnrichmentPort;
import dev.synapse.domain.task.EnrichedTaskContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JiraRestAdapter implements JiraEnrichmentPort {

    private final RestClient jiraRestClient;
    private final AdfParser adfParser;

    @Override
    public EnrichedTaskContext enrichTask(String ticketKey) {
        if (!StringUtils.hasText(ticketKey)) {
            throw new IllegalArgumentException("ticketKey cannot be null or blank");
        }
        String cleanTicketKey = ticketKey.trim();

        JiraIssue issue = getJiraIssue(cleanTicketKey);

        return new EnrichedTaskContext(
                cleanTicketKey,
                issue.fields().summary(),
                adfParser.parse(issue.fields().description()),
                List.of("Acceptance criteria 1: Must pass unit tests", "Acceptance criteria 2: Must satisfy security scan"),
                null
        );
    }

    private JiraIssue getJiraIssue(String ticketKey) {
        try {
            return jiraRestClient.get().uri("/rest/api/3/issue/{key}", ticketKey)
                    .retrieve().body(JiraIssue.class);
        } catch (Exception e) {
            log.error("Failed to get Jira issue: {}", ticketKey, e);
            throw new RuntimeException("Failed to get Jira issue: " + ticketKey, e);
        }
    }
}
