package com.lordkay.embabel.mcp.format;

import com.lordkay.embabel.mcp.config.McpContextProperties;
import com.lordkay.embabel.mcp.config.McpContextProperties.CompactMode;

/**
 * Optional response-size handling. Default is full fidelity: no truncation of biomedical tool output.
 */
public final class McpResponseCompactor {

    public enum ResponseKind {
        /** build_target_dossier, investigate_gene_symbol, markdown dossiers */
        workflow,
        /** ordinary search / get / compare / subgraph */
        standard
    }

    private McpResponseCompactor() {}

    /**
     * Applies policy after a tool has fully assembled its result (never mid-request).
     */
    public static String finish(String body, McpContextProperties config, ResponseKind kind) {
        if (body == null) {
            return "";
        }
        boolean workflowExempt =
                kind == ResponseKind.workflow && config.isExemptWorkflowToolsFromTruncation();

        if (!workflowExempt && config.getCompactMode() == CompactMode.truncate) {
            body = truncateIfNeeded(body, config.getMaxResponseChars());
        }

        if (config.getCompactMode() == CompactMode.warn
                || (config.getCompactMode() == CompactMode.off && body.length() > config.getWarnResponseChars())) {
            body = appendAdvisory(body, config.getWarnResponseChars());
        }

        return body;
    }

    /** @deprecated use {@link #finish} */
    public static String compact(String body, int maxChars) {
        if (body == null) {
            return "";
        }
        if (maxChars <= 0 || body.length() <= maxChars) {
            return body;
        }
        return truncateIfNeeded(body, maxChars);
    }

    private static String truncateIfNeeded(String body, int maxChars) {
        if (maxChars <= 0 || body.length() <= maxChars) {
            return body;
        }
        int keep = Math.max(500, maxChars - 400);
        return body.substring(0, keep)
                + "\n\n---\n\n**Truncated** — response exceeded "
                + maxChars
                + " characters. Prefer `format=markdown`, lower `limit`, or `build_target_dossier`. "
                + "Set `bioinsight.mcp.compact-mode=off` to disable truncation.";
    }

    private static String appendAdvisory(String body, int warnThreshold) {
        if (body.length() <= warnThreshold || body.contains("**Response size advisory**")) {
            return body;
        }
        int chars = body.length();
        int approxTokens = Math.max(1, chars / 4);
        return body
                + "\n\n---\n\n**Response size advisory** — "
                + chars
                + " characters (~"
                + approxTokens
                + " tokens at ~4 chars/token; actual count depends on the host model tokenizer). "
                + "Full content retained. If the chat feels crowded, start a new thread or use "
                + "`BIOINSIGHT_MCP_TOOL_PROFILE=minimal`.";
    }
}
