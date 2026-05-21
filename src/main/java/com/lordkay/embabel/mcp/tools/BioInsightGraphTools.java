package com.lordkay.embabel.mcp.tools;

import java.util.Map;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.lordkay.embabel.mcp.client.BioInsightApiClient;

/**
 * MCP tools that proxy the BioInsight Graph FastAPI.
 * Data is demo/sample Open Targets–style — not for clinical use.
 *
 * @see <a href="https://github.com/LordKay-sudo/bioinsight-graph">bioinsight-graph</a>
 */
@Component
public class BioInsightGraphTools {

    private final BioInsightApiClient api;

    public BioInsightGraphTools(BioInsightApiClient api) {
        this.api = api;
    }

    @McpTool(
            name = "bioinsight_health",
            description = "Check BioInsight Graph API liveness and Neo4j connectivity")
    public String health() {
        return api.get("/health");
    }

    @McpTool(
            name = "bioinsight_stats",
            description = "Count genes, diseases, proteins, and disease-target associations in the graph")
    public String stats() {
        return api.get("/stats");
    }

    @McpTool(
            name = "search_genes",
            description = "Search genes by symbol or name (e.g. BRCA1). Returns up to 25 matches.")
    public String searchGenes(
            @McpToolParam(description = "Search query (symbol or name)", required = true) String query) {
        return api.get("/genes", Map.of("q", query));
    }

    @McpTool(
            name = "search_diseases",
            description = "Search diseases by name or id. Returns up to 25 matches.")
    public String searchDiseases(
            @McpToolParam(description = "Search query (disease name or id)", required = true) String query) {
        return api.get("/diseases", Map.of("q", query));
    }

    @McpTool(
            name = "get_gene",
            description = "Gene metadata plus counts of linked diseases and proteins")
    public String getGene(
            @McpToolParam(description = "Ensembl gene id (e.g. ENSG00000012048)", required = true) String geneId) {
        return api.get("/genes/" + geneId);
    }

    @McpTool(
            name = "get_gene_neighbors",
            description = "One-hop neighborhood: diseases (with association scores) and encoding proteins")
    public String getGeneNeighbors(
            @McpToolParam(description = "Ensembl gene id", required = true) String geneId) {
        return api.get("/genes/" + geneId + "/neighbors");
    }

    @McpTool(
            name = "export_gene_subgraph",
            description = "Subgraph JSON for visualization (nodes + links centered on a gene)")
    public String exportGeneSubgraph(
            @McpToolParam(description = "Ensembl gene id", required = true) String geneId) {
        return api.get("/export/subgraph", Map.of("gene_id", geneId));
    }

    @McpTool(
            name = "investigate_gene_symbol",
            description =
                    "Search by symbol, then return gene detail + 1-hop neighbors for the best match (single JSON payload)")
    public String investigateGeneSymbol(
            @McpToolParam(description = "Gene symbol, e.g. BRCA1 or TP53", required = true) String symbol) {
        String searchJson = api.get("/genes", Map.of("q", symbol));
        if (searchJson.contains("\"error\":true")) {
            return searchJson;
        }
        String geneId = extractFirstGeneId(searchJson);
        if (geneId == null) {
            return "{\"error\":true,\"detail\":\"No gene found for symbol: " + symbol + "\"}";
        }
        String detail = api.get("/genes/" + geneId);
        String neighbors = api.get("/genes/" + geneId + "/neighbors");
        return """
                {"symbol":"%s","gene_id":"%s","detail":%s,"neighbors":%s}
                """
                .formatted(symbol, geneId, detail, neighbors);
    }

    /**
     * Minimal parse: first "id":"ENSG..." in search JSON array.
     */
    static String extractFirstGeneId(String searchJson) {
        int idx = searchJson.indexOf("\"id\":\"");
        if (idx < 0) {
            return null;
        }
        int start = idx + 6;
        int end = searchJson.indexOf('"', start);
        if (end < 0) {
            return null;
        }
        return searchJson.substring(start, end);
    }
}
