package com.lordkay.embabel.mcp.tools;

import java.util.Map;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.lordkay.embabel.mcp.client.BioInsightApiClient;
import com.lordkay.embabel.mcp.config.BioInsightProperties;
import com.lordkay.embabel.mcp.format.BioInsightMarkdown;
import com.lordkay.embabel.mcp.format.BioInsightProvenance;
import com.lordkay.embabel.mcp.util.GeneIdParser;

/**
 * MCP tools that proxy the BioInsight Graph FastAPI.
 * Data is demo/sample Open Targets–style — not for clinical use.
 */
@Component
public class BioInsightGraphTools {

    private final BioInsightApiClient api;
    private final BioInsightProperties properties;

    public BioInsightGraphTools(BioInsightApiClient api, BioInsightProperties properties) {
        this.api = api;
        this.properties = properties;
    }

    @McpTool(name = "bioinsight_health", description = "Check BioInsight Graph API liveness and Neo4j connectivity")
    public String health(
            @McpToolParam(description = "Response format: markdown or json", required = false) String format) {
        return respond(api.get("/health"), format);
    }

    @McpTool(name = "bioinsight_stats", description = "Count genes, diseases, proteins, and disease-target associations")
    public String stats(
            @McpToolParam(description = "Response format: markdown or json", required = false) String format) {
        return respond(api.get("/stats"), format);
    }

    @McpTool(
            name = "search_genes",
            description = "Search genes by symbol or name (e.g. BRCA1). Returns up to 25 matches.")
    public String searchGenes(
            @McpToolParam(description = "Search query (symbol or name)", required = true) String query,
            @McpToolParam(description = "Response format: markdown or json", required = false) String format) {
        return respond(api.get("/genes", Map.of("q", query)), format);
    }

    @McpTool(name = "search_diseases", description = "Search diseases by name or id. Returns up to 25 matches.")
    public String searchDiseases(
            @McpToolParam(description = "Search query", required = true) String query,
            @McpToolParam(description = "Response format: markdown or json", required = false) String format) {
        return respond(api.get("/diseases", Map.of("q", query)), format);
    }

    @McpTool(name = "get_gene", description = "Gene metadata plus counts of linked diseases and proteins")
    public String getGene(
            @McpToolParam(description = "Ensembl gene id", required = true) String geneId,
            @McpToolParam(description = "Response format: markdown or json", required = false) String format) {
        return respond(api.get("/genes/" + geneId), format);
    }

    @McpTool(
            name = "get_gene_diseases",
            description = "Diseases linked to a gene, ranked by association score (Open Targets–style)")
    public String getGeneDiseases(
            @McpToolParam(description = "Ensembl gene id", required = true) String geneId,
            @McpToolParam(description = "Minimum association score 0–1", required = false) Double minScore,
            @McpToolParam(description = "Max results (default 25)", required = false) Integer limit,
            @McpToolParam(description = "Response format: markdown or json", required = false) String format) {
        return respond(
                api.get(
                        "/genes/" + geneId + "/diseases",
                        Map.of(
                                "min_score", String.valueOf(minScore != null ? minScore : 0.0),
                                "limit", String.valueOf(limit != null ? limit : 25))),
                format);
    }

    @McpTool(name = "get_disease", description = "Disease metadata and count of linked gene targets")
    public String getDisease(
            @McpToolParam(description = "Disease id (e.g. MONDO_...)", required = true) String diseaseId,
            @McpToolParam(description = "Response format: markdown or json", required = false) String format) {
        return respond(api.get("/diseases/" + diseaseId), format);
    }

    @McpTool(
            name = "get_disease_genes",
            description = "Gene targets for a disease ranked by association score (top targets for disease)")
    public String getDiseaseGenes(
            @McpToolParam(description = "Disease id", required = true) String diseaseId,
            @McpToolParam(description = "Minimum association score 0–1", required = false) Double minScore,
            @McpToolParam(description = "Max results (default 25)", required = false) Integer limit,
            @McpToolParam(description = "Response format: markdown or json", required = false) String format) {
        return respond(
                api.get(
                        "/diseases/" + diseaseId + "/genes",
                        Map.of(
                                "min_score", String.valueOf(minScore != null ? minScore : 0.0),
                                "limit", String.valueOf(limit != null ? limit : 25))),
                format);
    }

    @McpTool(
            name = "compare_genes",
            description = "Compare 2–5 genes: top diseases each and overlapping disease names")
    public String compareGenes(
            @McpToolParam(description = "Comma-separated symbols, e.g. BRCA1,TP53", required = true) String symbols,
            @McpToolParam(description = "Top N diseases per gene (default 5)", required = false) Integer topN,
            @McpToolParam(description = "Response format: markdown or json", required = false) String format) {
        return respond(
                api.get(
                        "/genes/compare",
                        Map.of(
                                "symbols", symbols,
                                "top_n", String.valueOf(topN != null ? topN : 5))),
                format);
    }

    @McpTool(
            name = "get_gene_neighbors",
            description = "One-hop neighborhood: diseases (with scores) and encoding proteins")
    public String getGeneNeighbors(
            @McpToolParam(description = "Ensembl gene id", required = true) String geneId,
            @McpToolParam(description = "Response format: markdown or json", required = false) String format) {
        return respond(api.get("/genes/" + geneId + "/neighbors"), format);
    }

    @McpTool(name = "export_gene_subgraph", description = "Subgraph JSON for visualization (nodes + links)")
    public String exportGeneSubgraph(
            @McpToolParam(description = "Ensembl gene id", required = true) String geneId,
            @McpToolParam(description = "Response format: markdown or json (json recommended)", required = false)
                    String format) {
        return respond(api.get("/export/subgraph", Map.of("gene_id", geneId)), format, true);
    }

    @McpTool(
            name = "build_target_dossier",
            description =
                    """
                    Workflow: resolve a gene symbol → detail, ranked diseases, neighborhood, live stats.
                    Returns one markdown report with provenance footer and UI deep link.
                    Use when handing off a target investigation (not for clinical decisions).
                    Requires BioInsight API on BIOINSIGHT_API_BASE_URL.""")
    public String buildTargetDossier(
            @McpToolParam(description = "Gene symbol, e.g. BRCA1", required = true) String symbol,
            @McpToolParam(description = "Max ranked diseases (default 15)", required = false) Integer diseaseLimit,
            @McpToolParam(description = "Response format: markdown or json", required = false) String format) {
        String searchJson = api.get("/genes", Map.of("q", symbol));
        if (searchJson.contains("\"error\":true")) {
            return respond(searchJson, format);
        }
        String geneId = GeneIdParser.extractFirstGeneId(searchJson);
        if (geneId == null) {
            return respond("{\"error\":true,\"detail\":\"No gene found for symbol: " + symbol + "\"}", format);
        }
        String detail = api.get("/genes/" + geneId);
        String diseases =
                api.get(
                        "/genes/" + geneId + "/diseases",
                        Map.of("limit", String.valueOf(diseaseLimit != null ? diseaseLimit : 15)));
        String neighbors = api.get("/genes/" + geneId + "/neighbors");
        String stats = api.get("/stats");
        String payload =
                """
                {"symbol":"%s","gene_id":"%s","detail":%s,"diseases":%s,"neighbors":%s,"stats":%s}
                """
                        .formatted(symbol, geneId, detail, diseases, neighbors, stats);
        if (wantsMarkdown(format)) {
            return markdownDossier(symbol, geneId, detail, diseases, neighbors, stats);
        }
        return payload;
    }

    @McpTool(
            name = "investigate_gene_symbol",
            description =
                    """
                    Search by symbol, then return gene detail + ranked diseases + neighbors.
                    Lighter than build_target_dossier (no stats block). Prefer build_target_dossier for full handoff.""")
    public String investigateGeneSymbol(
            @McpToolParam(description = "Gene symbol, e.g. BRCA1", required = true) String symbol,
            @McpToolParam(description = "Response format: markdown or json", required = false) String format) {
        String searchJson = api.get("/genes", Map.of("q", symbol));
        if (searchJson.contains("\"error\":true")) {
            return respond(searchJson, format);
        }
        String geneId = GeneIdParser.extractFirstGeneId(searchJson);
        if (geneId == null) {
            return respond("{\"error\":true,\"detail\":\"No gene found for symbol: " + symbol + "\"}", format);
        }
        String detail = api.get("/genes/" + geneId);
        String diseases = api.get("/genes/" + geneId + "/diseases", Map.of("limit", "10"));
        String neighbors = api.get("/genes/" + geneId + "/neighbors");
        String payload =
                """
                {"symbol":"%s","gene_id":"%s","detail":%s,"diseases":%s,"neighbors":%s}
                """
                        .formatted(symbol, geneId, detail, diseases, neighbors);
        if (wantsMarkdown(format)) {
            return markdownDossier(symbol, geneId, detail, diseases, neighbors, null);
        }
        return payload;
    }

    private String markdownDossier(
            String symbol,
            String geneId,
            String detail,
            String diseases,
            String neighbors,
            String stats) {
        StringBuilder sb = new StringBuilder("# Target dossier: ").append(symbol).append("\n\n");
        sb.append(BioInsightMarkdown.format(detail)).append("\n");
        sb.append(BioInsightMarkdown.format(diseases)).append("\n");
        sb.append(BioInsightMarkdown.format(neighbors)).append("\n");
        if (stats != null) {
            sb.append(BioInsightMarkdown.format(stats)).append("\n");
        }
        return sb.append(BioInsightProvenance.footer(api, properties.getWebUiBaseUrl(), geneId)).toString();
    }

    private static String respond(String json, String format) {
        return respond(json, format, false);
    }

    private static String respond(String json, String format, boolean defaultJson) {
        if (wantsMarkdown(format) || (!defaultJson && !wantsJson(format))) {
            return BioInsightMarkdown.format(json);
        }
        return json;
    }

    private static boolean wantsMarkdown(String format) {
        return format != null && format.equalsIgnoreCase("markdown");
    }

    private static boolean wantsJson(String format) {
        return format != null && format.equalsIgnoreCase("json");
    }
}
