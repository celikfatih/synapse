package dev.synapse.adapter.out.jira.model.adf;

import java.util.Map;

public record AdfMark(String type, Map<String, Object> attrs) {
}
