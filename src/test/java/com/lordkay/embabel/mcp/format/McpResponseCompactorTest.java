package com.lordkay.embabel.mcp.format;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class McpResponseCompactorTest {

    @Test
    void compact_truncatesWhenOverLimit() {
        String body = "x".repeat(20_000);
        String out = McpResponseCompactor.compact(body, 1000);
        assertTrue(out.length() < body.length());
        assertTrue(out.contains("Truncated"));
    }

    @Test
    void compact_leavesSmallResponses() {
        String body = "ok";
        assertTrue(McpResponseCompactor.compact(body, 1000).equals("ok"));
    }
}
