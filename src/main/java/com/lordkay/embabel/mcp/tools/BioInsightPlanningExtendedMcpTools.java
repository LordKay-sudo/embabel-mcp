package com.lordkay.embabel.mcp.tools;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.lordkay.embabel.mcp.client.BioInsightApiClient;
import com.lordkay.embabel.mcp.config.BioInsightProperties;
import com.lordkay.embabel.mcp.config.McpContextProperties;
import com.lordkay.embabel.mcp.util.IdentifierResolver;
import com.lordkay.embabel.mcp.util.IdentifierResolver.EntityType;
import com.lordkay.embabel.mcp.util.ProvenanceBundleExporter;
import com.lordkay.embabel.mcp.util.TargetEvidenceFetcher;

/** M3, M4 — resolution and evidence tools (standard and full profiles). */
@Component
@ConditionalOnExpression("'${bioinsight.mcp.tool-profile:standard}' != 'minimal'")
public class BioInsightPlanningExtendedMcpTools extends BioInsightToolSupport {

    public BioInsightPlanningExtendedMcpTools(
            BioInsightApiClient api, BioInsightProperties properties, McpContextProperties context) {
        super(api, properties, context);
    }

    @McpTool(
            name = "resolve_identifier",
            description = "Resolve gene symbol or disease name to canonical id with ambiguity notes.")
    public String resolveIdentifier(
            @McpToolParam(description = "Symbol, name, or id fragment", required = true) String query,
            @McpToolParam(description = "gene or disease", required = true) String entityType,
            @McpToolParam(description = "markdown or json", required = false) String format) {
        EntityType type = "disease".equalsIgnoreCase(entityType) ? EntityType.DISEASE : EntityType.GENE;
        String json = IdentifierResolver.resolve(api, query, type);
        return respond(json, format, true);
    }

    @McpTool(
            name = "get_target_evidence",
            description =
                    "Evidence for gene–disease association(s). Uses API breakdown when available; else association scores.")
    public String getTargetEvidence(
            @McpToolParam(description = "Gene symbol or ENSG id", required = true) String geneIdOrSymbol,
            @McpToolParam(description = "Optional disease id to filter one association", required = false)
                    String diseaseId,
            @McpToolParam(description = "Max associations when disease_id omitted", required = false) Integer limit,
            @McpToolParam(description = "markdown or json", required = false) String format) {
        int cap = limit != null ? limit : context.getDefaultDiseaseLimit();
        String json = TargetEvidenceFetcher.fetch(api, geneIdOrSymbol, diseaseId, cap);
        return respond(json, format, true);
    }

    @McpTool(
            name = "get_gene_external_links",
            description = "Federated links: Ensembl, Open Targets, UniProt (BioInsight 2.2).")
    public String getGeneExternalLinks(
            @McpToolParam(description = "Gene symbol or ENSG id", required = true) String geneIdOrSymbol,
            @McpToolParam(description = "markdown or json", required = false) String format) {
        String geneId = resolveEnsg(api, geneIdOrSymbol);
        if (geneId == null) {
            return respond("{\"error\":true,\"detail\":\"Gene not found: " + geneIdOrSymbol + "\"}", format);
        }
        return respond(api.get("/genes/" + geneId + "/external-links"), format);
    }

    @McpTool(
            name = "export_provenance_bundle",
            description = "M7: JSON audit bundle — meta, resolution, external links, evidence sample, UI URL.")
    public String exportProvenanceBundle(
            @McpToolParam(description = "Gene symbol or ENSG id", required = true) String geneIdOrSymbol,
            @McpToolParam(description = "markdown or json (default json)", required = false) String format) {
        String json = ProvenanceBundleExporter.export(api, properties.getWebUiBaseUrl(), geneIdOrSymbol);
        return respond(json, format, true);
    }

    @McpTool(
            name = "batch_gene_lookup",
            description = "Resolve many gene symbols/ids at once (BioInsight 3.4). Returns hits + unresolved.")
    public String batchGeneLookup(
            @McpToolParam(description = "Comma-separated symbols or ENSG ids", required = true) String symbols,
            @McpToolParam(description = "markdown or json", required = false) String format) {
        String[] parts = symbols.split(",");
        StringBuilder arr = new StringBuilder("[");
        boolean first = true;
        for (String p : parts) {
            String q = p.trim();
            if (q.isEmpty()) {
                continue;
            }
            if (!first) {
                arr.append(",");
            }
            arr.append("\"").append(q.replace("\"", "\\\"")).append("\"");
            first = false;
        }
        arr.append("]");
        String body = "{\"queries\":" + arr + "}";
        return respond(api.postJson("/genes/batch-lookup", body), format, true);
    }

    @McpTool(
            name = "export_gene_report",
            description = "Analyst gene report with provenance columns (BioInsight 3.5). JSON for agents.")
    public String exportGeneReport(
            @McpToolParam(description = "Gene symbol or ENSG id", required = true) String geneIdOrSymbol,
            @McpToolParam(description = "markdown or json", required = false) String format) {
        String geneId = resolveEnsg(api, geneIdOrSymbol);
        if (geneId == null) {
            return respond("{\"error\":true,\"detail\":\"Gene not found: " + geneIdOrSymbol + "\"}", format);
        }
        return respond(
                api.get("/export/gene-report", java.util.Map.of("gene_id", geneId, "format", "json")),
                format,
                true);
    }

    private static String resolveEnsg(BioInsightApiClient api, String geneIdOrSymbol) {
        if (geneIdOrSymbol.matches("ENSG\\d{11}")) {
            return geneIdOrSymbol;
        }
        String resolved = IdentifierResolver.resolveGene(api, geneIdOrSymbol);
        if (resolved.contains("\"error\":true")) {
            return null;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(resolved)
                    .get("canonical_id")
                    .asText();
        } catch (Exception ex) {
            return null;
        }
    }
}
