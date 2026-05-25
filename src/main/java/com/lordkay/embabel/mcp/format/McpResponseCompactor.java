package com.lordkay.embabel.mcp.format;

/**
 * Truncates oversized tool outputs so a single call cannot dominate the conversation context window.
 */
public final class McpResponseCompactor {

    private McpResponseCompactor() {}

    public static String compact(String body, int maxChars) {
        if (body == null) {
            return "";
        }
        if (maxChars <= 0 || body.length() <= maxChars) {
            return body;
        }
        int keep = Math.max(500, maxChars - 400);
        return body.substring(0, keep)
                + "\n\n---\n\n**Truncated** — response exceeded "
                + maxChars
                + " characters. Use a narrower query, lower `limit`, `format=markdown`, or "
                + "`build_target_dossier` instead of raw JSON subgraph export.";
    }
}
