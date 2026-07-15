package dev.synapse.adapter.out.jira.model.adf;

import java.util.List;

// Atlassian Document Format(ADF models)
public record AdfDocument(String type, String text, List<AdfNode> content) {
}
