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
}
