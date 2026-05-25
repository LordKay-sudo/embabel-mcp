package com.lordkay.embabel.mcp.tools;

import java.util.Map;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.lordkay.embabel.mcp.client.BioInsightApiClient;
import com.lordkay.embabel.mcp.config.BioInsightProperties;
import com.lordkay.embabel.mcp.config.McpContextProperties;

/** Granular entity lookups — disabled when {@code bioinsight.mcp.tool-profile=minimal}. */
@Component
@ConditionalOnExpression("'${bioinsight.mcp.tool-profile:standard}' != 'minimal'")
public class BioInsightExtendedMcpTools extends BioInsightToolSupport {

    public BioInsightExtendedMcpTools(
            BioInsightApiClient api, BioInsightProperties properties, McpContextProperties context) {
        super(api, properties, context);
    }

    @McpTool(name = "get_gene", description = "Gene metadata and link counts by Ensembl id")
    public String getGene(
            @McpToolParam(description = "Ensembl gene id", required = true) String geneId,
            @McpToolParam(description = "markdown or json", required = false) String format) {
        return respond(api.get("/genes/" + geneId), format);
    }

    @McpTool(name = "get_gene_diseases", description = "Ranked diseases for a gene")
    public String getGeneDiseases(
            @McpToolParam(description = "Ensembl gene id", required = true) String geneId,
            @McpToolParam(description = "Min score 0–1", required = false) Double minScore,
            @McpToolParam(description = "Max results (default 10)", required = false) Integer limit,
            @McpToolParam(description = "markdown or json", required = false) String format) {
        int cap = limit != null ? limit : context.getDefaultDiseaseLimit();
        return respond(
                api.get(
                        "/genes/" + geneId + "/diseases",
                        Map.of(
                                "min_score", String.valueOf(minScore != null ? minScore : 0.0),
                                "limit", String.valueOf(cap))),
                format);
    }

    @McpTool(name = "get_disease", description = "Disease metadata and target count")
    public String getDisease(
            @McpToolParam(description = "Disease id", required = true) String diseaseId,
            @McpToolParam(description = "markdown or json", required = false) String format) {
        return respond(api.get("/diseases/" + diseaseId), format);
    }

    @McpTool(name = "get_disease_genes", description = "Ranked gene targets for a disease")
    public String getDiseaseGenes(
            @McpToolParam(description = "Disease id", required = true) String diseaseId,
            @McpToolParam(description = "Min score 0–1", required = false) Double minScore,
            @McpToolParam(description = "Max results (default 10)", required = false) Integer limit,
            @McpToolParam(description = "markdown or json", required = false) String format) {
        int cap = limit != null ? limit : context.getDefaultDiseaseLimit();
        return respond(
                api.get(
                        "/diseases/" + diseaseId + "/genes",
                        Map.of(
                                "min_score", String.valueOf(minScore != null ? minScore : 0.0),
                                "limit", String.valueOf(cap))),
                format);
    }

    @McpTool(name = "compare_genes", description = "Compare 2–5 symbols; overlap in top diseases")
    public String compareGenes(
            @McpToolParam(description = "Comma-separated symbols", required = true) String symbols,
            @McpToolParam(description = "Top N per gene (default 5)", required = false) Integer topN,
            @McpToolParam(description = "markdown or json", required = false) String format) {
        return respond(
                api.get(
                        "/genes/compare",
                        Map.of("symbols", symbols, "top_n", String.valueOf(topN != null ? topN : 5))),
                format);
    }
}
