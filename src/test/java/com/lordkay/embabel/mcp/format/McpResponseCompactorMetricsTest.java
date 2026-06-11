package com.lordkay.embabel.mcp.format;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.lordkay.embabel.mcp.config.McpContextProperties;
import com.lordkay.embabel.mcp.config.McpContextProperties.CompactMode;

class McpResponseCompactorMetricsTest {

    @Test
    void finish_emitsMetricsWithoutTruncatingWorkflow() {
        McpContextProperties config = new McpContextProperties();
        config.setToolProfile(McpContextProperties.ToolProfile.standard);
        config.setCompactMode(CompactMode.off);
        config.setWarnResponseChars(10);
        config.setExemptWorkflowToolsFromTruncation(true);

        String out =
                McpResponseCompactor.finish(
                        "x".repeat(50), config, McpResponseCompactor.ResponseKind.workflow);
        assertTrue(out.contains("Response size advisory") || out.length() >= 50);
    }
}
