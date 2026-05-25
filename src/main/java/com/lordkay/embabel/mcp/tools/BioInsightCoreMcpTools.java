package com.lordkay.embabel.mcp.tools;

import java.util.Map;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.lordkay.embabel.mcp.client.BioInsightApiClient;
import com.lordkay.embabel.mcp.config.BioInsightProperties;
import com.lordkay.embabel.mcp.config.McpContextProperties;
import com.lordkay.embabel.mcp.format.McpResponseCompactor;
import com.lordkay.embabel.mcp.util.GeneIdParser;

/**
 * Always-on MCP tools — kept small for low context footprint ({@code tool-profile=minimal}).
 */
@Component
public class BioInsightCoreMcpTools extends BioInsightToolSupport {

    public BioInsightCoreMcpTools(
            BioInsightApiClient api, BioInsightProperties properties, McpContextProperties context) {
        super(api, properties, context);
    }

    @McpTool(name = "bioinsight_health", description = "API + Neo4j liveness")
    public String health(
            @McpToolParam(description = "markdown (default) or json", required = false) String format) {
        return respond(api.get("/health"), format);
    }

    @McpTool(name = "bioinsight_stats", description = "Graph node/edge counts")
    public String stats(
            @McpToolParam(description = "markdown (default) or json", required = false) String format) {
        return respond(api.get("/stats"), format);
    }

    @McpTool(name = "search_genes", description = "Search genes by symbol or name (max 25)")
    public String searchGenes(
            @McpToolParam(description = "Query", required = true) String query,
            @McpToolParam(description = "markdown or json", required = false) String format) {
        return respond(api.get("/genes", Map.of("q", query)), format);
    }

    @McpTool(name = "search_diseases", description = "Search diseases by name or id (max 25)")
    public String searchDiseases(
            @McpToolParam(description = "Query", required = true) String query,
            @McpToolParam(description = "markdown or json", required = false) String format) {
        return respond(api.get("/diseases", Map.of("q", query)), format);
    }

    @McpTool(
            name = "build_target_dossier",
            description =
                    "Preferred workflow: symbol → ranked diseases + neighborhood + stats + provenance (markdown).")
    public String buildTargetDossier(
            @McpToolParam(description = "Gene symbol, e.g. BRCA1", required = true) String symbol,
            @McpToolParam(description = "Max diseases (default from server config)", required = false)
                    Integer diseaseLimit,
            @McpToolParam(description = "markdown (default) or json", required = false) String format) {
        int limit = diseaseLimit != null ? diseaseLimit : context.getDefaultDiseaseLimit();
        String searchJson = api.get("/genes", Map.of("q", symbol));
        if (searchJson.contains("\"error\":true")) {
            return respond(searchJson, format);
        }
        String geneId = GeneIdParser.extractFirstGeneId(searchJson);
        if (geneId == null) {
            return respond("{\"error\":true,\"detail\":\"No gene found for symbol: " + symbol + "\"}", format);
        }
        String detail = api.get("/genes/" + geneId);
        String diseases = api.get("/genes/" + geneId + "/diseases", Map.of("limit", String.valueOf(limit)));
        String neighbors = api.get("/genes/" + geneId + "/neighbors");
        String stats = api.get("/stats");
        if (wantsMarkdown(format)) {
            return markdownDossier(symbol, geneId, detail, diseases, neighbors, stats);
        }
        String payload =
                """
                {"symbol":"%s","gene_id":"%s","detail":%s,"diseases":%s,"neighbors":%s,"stats":%s}
                """
                        .formatted(symbol, geneId, detail, diseases, neighbors, stats);
        return McpResponseCompactor.finish(payload, context, McpResponseCompactor.ResponseKind.workflow);
    }
}
