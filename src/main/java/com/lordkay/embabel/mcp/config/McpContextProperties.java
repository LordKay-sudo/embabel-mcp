package com.lordkay.embabel.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limits MCP payload size and controls how many tools are registered per profile.
 */
@ConfigurationProperties(prefix = "bioinsight.mcp")
public class McpContextProperties {

    /**
     * minimal = dossier + search + health/stats only; standard = + compare and entity lookups;
     * full = + neighbors, subgraph export, investigate.
     */
    private ToolProfile toolProfile = ToolProfile.standard;

    /** Hard cap on characters returned from any single tool call (markdown or json). */
    private int maxResponseChars = 16_000;

    /** Default disease limit for dossier when caller omits diseaseLimit. */
    private int defaultDiseaseLimit = 10;

    public enum ToolProfile {
        minimal,
        standard,
        full
    }

    public ToolProfile getToolProfile() {
        return toolProfile;
    }

    public void setToolProfile(ToolProfile toolProfile) {
        this.toolProfile = toolProfile;
    }

    public int getMaxResponseChars() {
        return maxResponseChars;
    }

    public void setMaxResponseChars(int maxResponseChars) {
        this.maxResponseChars = maxResponseChars;
    }

    public int getDefaultDiseaseLimit() {
        return defaultDiseaseLimit;
    }

    public void setDefaultDiseaseLimit(int defaultDiseaseLimit) {
        this.defaultDiseaseLimit = defaultDiseaseLimit;
    }
}
