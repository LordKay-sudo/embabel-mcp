package com.lordkay.embabel.mcp.tools;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.lordkay.embabel.mcp.client.KgRagApiClient;

/**
 * Optional bridge to {@link com.lordkay.embabel.mcp.config.KgRagProperties kg-rag-demo} for
 * citation-grounded literature Q&amp;A alongside structured graph tools.
 */
@Component
@ConditionalOnProperty(prefix = "kg-rag", name = "enabled", havingValue = "true")
public class KgRagBridgeTools {

    private final KgRagApiClient api;

    public KgRagBridgeTools(KgRagApiClient api) {
        this.api = api;
    }

    @McpTool(
            name = "kg_rag_health",
            description = "Check KG RAG Demo API (documents + vector RAG) when kg-rag is running on :8001")
    public String health() {
        return api.health();
    }

    @McpTool(
            name = "kg_rag_ask",
            description =
                    "Ask the document corpus a biomedical question with citations (requires kg-rag-demo API)")
    public String ask(
            @McpToolParam(description = "Natural language question", required = true) String question) {
        return api.ask(question);
    }
}
