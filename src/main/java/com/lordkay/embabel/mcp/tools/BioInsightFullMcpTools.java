package com.lordkay.embabel.mcp.tools;

import java.util.Map;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.lordkay.embabel.mcp.client.BioInsightApiClient;
import com.lordkay.embabel.mcp.config.BioInsightProperties;
import com.lordkay.embabel.mcp.config.McpContextProperties;

/**
 * High-volume tools — only when {@code bioinsight.mcp.tool-profile=full}. Prefer {@code build_target_dossier}
 * in minimal/standard profiles.
 */
@Component
@ConditionalOnExpression("'${bioinsight.mcp.tool-profile:standard}' == 'full'")
public class BioInsightFullMcpTools extends BioInsightToolSupport {

    public BioInsightFullMcpTools(
            BioInsightApiClient api, BioInsightProperties properties, McpContextProperties context) {
        super(api, properties, context);
    }

    @McpTool(name = "get_gene_neighbors", description = "One-hop graph neighborhood (can be large)")
    public String getGeneNeighbors(
            @McpToolParam(description = "Ensembl gene id", required = true) String geneId,
            @McpToolParam(description = "markdown or json", required = false) String format) {
        return respond(api.get("/genes/" + geneId + "/neighbors"), format);
    }

    @McpTool(
            name = "export_gene_subgraph",
            description = "Raw subgraph JSON — high token cost; use only when visualization needs JSON")
    public String exportGeneSubgraph(
            @McpToolParam(description = "Ensembl gene id", required = true) String geneId,
            @McpToolParam(description = "json recommended", required = false) String format) {
        return respond(api.get("/export/subgraph", Map.of("gene_id", geneId)), format, true);
    }

    @McpTool(
            name = "investigate_gene_symbol",
            description = "Legacy composite; prefer build_target_dossier unless profile=full")
    public String investigateGeneSymbol(
            @McpToolParam(description = "Gene symbol", required = true) String symbol,
            @McpToolParam(description = "markdown or json", required = false) String format) {
        String geneId = resolveGeneId(symbol);
        if (geneId == null) {
            return respond("{\"error\":true,\"detail\":\"No gene found for symbol: " + symbol + "\"}", format);
        }
        String detail = api.get("/genes/" + geneId);
        String diseases =
                api.get(
                        "/genes/" + geneId + "/diseases",
                        Map.of("limit", String.valueOf(context.getDefaultDiseaseLimit())));
        String neighbors = api.get("/genes/" + geneId + "/neighbors");
        if (wantsMarkdown(format)) {
            return markdownDossier(symbol, geneId, detail, diseases, neighbors, null);
        }
        return respond(
                """
                {"symbol":"%s","gene_id":"%s","detail":%s,"diseases":%s,"neighbors":%s}
                """
                        .formatted(symbol, geneId, detail, diseases, neighbors),
                format);
    }
}
