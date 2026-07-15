package dev.synapse.adapter.out.jira.parser;

import dev.synapse.adapter.out.jira.model.adf.AdfDocument;

public interface AdfParser {
    /**
     * Parses an ADF document into a plain text string.
     * @param document the ADF document to be parsed.
     * @return the parsed text.
     */
    String parse(AdfDocument document);
}
