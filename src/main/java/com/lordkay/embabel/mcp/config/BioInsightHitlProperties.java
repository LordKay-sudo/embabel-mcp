package com.lordkay.embabel.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bioinsight.hitl")
public class BioInsightHitlProperties {

    /**
     * When true, {@link com.lordkay.embabel.mcp.agent.GeneResearchAgent} pauses for
     * {@link com.lordkay.embabel.mcp.domain.GeneResearchApproval} via Embabel WaitFor.
     * Keep false for MCP/SSE (Cursor) unless your client supports form submission.
     */
    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
