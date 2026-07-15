package dev.synapse.adapter.out.jira.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JiraIssue(String id, String key, JiraFields fields) {
}

