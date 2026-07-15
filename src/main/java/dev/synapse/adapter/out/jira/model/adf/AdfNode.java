package dev.synapse.adapter.out.jira.model.adf;

import java.util.List;
import java.util.Map;

public record AdfNode(String type, String text, List<AdfNode> content, List<AdfMark> marks, Map<String, Object> attrs) {
}
