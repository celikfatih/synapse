package dev.synapse.adapter.out.jira.parser;

import dev.synapse.adapter.out.jira.model.adf.AdfDocument;
import dev.synapse.adapter.out.jira.model.adf.AdfNode;
import org.springframework.stereotype.Component;

@Component
public class AdfPlainTextParser implements AdfParser {

    @Override
    public String parse(AdfDocument document) {
        StringBuilder builder = new StringBuilder();

        for (AdfNode node : document.content()) {
            visit(node, builder);
        }

        return builder.toString().trim();
    }

    private void visit(AdfNode node, StringBuilder builder) {
        switch (node.type()) {
            case "paragraph" -> {
                visitChildren(node, builder);
                builder.append('\n');
            }
            case "text" -> builder.append(node.text());
            default -> visitChildren(node, builder);
        }
    }

    private void visitChildren(AdfNode node, StringBuilder builder) {
        if (node.content() == null) {
            return;
        }

        for (AdfNode child : node.content()) {
            visit(child, builder);
        }
    }
}
