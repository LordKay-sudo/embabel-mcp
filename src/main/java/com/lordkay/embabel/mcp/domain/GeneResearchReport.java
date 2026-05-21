package com.lordkay.embabel.mcp.domain;

/**
 * Final markdown report returned by {@link com.lordkay.embabel.mcp.agent.GeneResearchAgent}.
 */
public record GeneResearchReport(String markdown) {

    @Override
    public String toString() {
        return markdown;
    }
}
