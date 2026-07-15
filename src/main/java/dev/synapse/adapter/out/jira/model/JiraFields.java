package dev.synapse.adapter.out.jira.model;

import dev.synapse.adapter.out.jira.model.adf.AdfDocument;

public record JiraFields(String summary, AdfDocument description) {
}
