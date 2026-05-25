package com.lordkay.embabel.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Controls MCP tool surface area and optional response-size guidance (not hard cuts by default).
 */
@ConfigurationProperties(prefix = "bioinsight.mcp")
public class McpContextProperties {

    /**
     * minimal = dossier + search + health/stats only; standard = + compare and entity lookups;
     * full = + neighbors, subgraph export, investigate.
     */
    private ToolProfile toolProfile = ToolProfile.standard;

    /**
     * off = never truncate (default for this project). warn = append size notice only. truncate = hard
     * cap using maxResponseChars (opt-in emergency only).
     */
    private CompactMode compactMode = CompactMode.off;

    /**
     * Used only when compactMode=truncate. 0 means unlimited even in truncate mode. Workflow dossier
     * tools are never truncated regardless.
     */
    private int maxResponseChars = 0;

    /**
     * When compactMode=warn (or off), append an advisory footer if the response exceeds this size.
     * Does not remove content.
     */
    private int warnResponseChars = 12_000;

    /** Workflow tools (dossier / investigate) skip truncate even when compactMode=truncate. */
    private boolean exemptWorkflowToolsFromTruncation = true;

    /** Default disease limit for dossier when caller omits diseaseLimit. */
    private int defaultDiseaseLimit = 10;

    public enum ToolProfile {
        minimal,
        standard,
        full
    }

    public enum CompactMode {
        off,
        warn,
        truncate
    }

    public ToolProfile getToolProfile() {
        return toolProfile;
    }

    public void setToolProfile(ToolProfile toolProfile) {
        this.toolProfile = toolProfile;
    }

    public CompactMode getCompactMode() {
        return compactMode;
    }

    public void setCompactMode(CompactMode compactMode) {
        this.compactMode = compactMode;
    }

    public int getMaxResponseChars() {
        return maxResponseChars;
    }

    public void setMaxResponseChars(int maxResponseChars) {
        this.maxResponseChars = maxResponseChars;
    }

    public int getWarnResponseChars() {
        return warnResponseChars;
    }

    public void setWarnResponseChars(int warnResponseChars) {
        this.warnResponseChars = warnResponseChars;
    }

    public boolean isExemptWorkflowToolsFromTruncation() {
        return exemptWorkflowToolsFromTruncation;
    }

    public void setExemptWorkflowToolsFromTruncation(boolean exemptWorkflowToolsFromTruncation) {
        this.exemptWorkflowToolsFromTruncation = exemptWorkflowToolsFromTruncation;
    }

    public int getDefaultDiseaseLimit() {
        return defaultDiseaseLimit;
    }

    public void setDefaultDiseaseLimit(int defaultDiseaseLimit) {
        this.defaultDiseaseLimit = defaultDiseaseLimit;
    }
}
