package com.lordkay.embabel.mcp.format;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.lordkay.embabel.mcp.config.McpContextProperties;
import com.lordkay.embabel.mcp.config.McpContextProperties.CompactMode;

class McpResponseCompactorTest {

    @Test
    void workflow_neverTruncatesEvenInTruncateMode() {
        McpContextProperties config = new McpContextProperties();
        config.setCompactMode(CompactMode.truncate);
        config.setMaxResponseChars(100);
        String body = "x".repeat(500);
        String out = McpResponseCompactor.finish(body, config, McpResponseCompactor.ResponseKind.workflow);
        assertTrue(out.length() >= 500);
        assertFalse(out.contains("Truncated"));
    }

    @Test
    void standardTruncateOnlyWhenExplicitlyEnabled() {
        McpContextProperties config = new McpContextProperties();
        config.setCompactMode(CompactMode.truncate);
        config.setMaxResponseChars(1000);
        String body = "y".repeat(5000);
        String out = McpResponseCompactor.finish(body, config, McpResponseCompactor.ResponseKind.standard);
        assertTrue(out.contains("Truncated"));
        assertTrue(out.length() < 5000);
    }

    @Test
    void offModeRetainsFullBodyAndMayAdvise() {
        McpContextProperties config = new McpContextProperties();
        config.setCompactMode(CompactMode.off);
        config.setWarnResponseChars(100);
        String body = "z".repeat(200);
        String out = McpResponseCompactor.finish(body, config, McpResponseCompactor.ResponseKind.standard);
        assertTrue(out.contains("zzz"));
        assertTrue(out.contains("Response size advisory"));
    }
}
