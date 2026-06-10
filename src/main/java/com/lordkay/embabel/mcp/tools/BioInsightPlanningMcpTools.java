package com.lordkay.embabel.mcp.tools;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.lordkay.embabel.mcp.client.BioInsightApiClient;
import com.lordkay.embabel.mcp.config.BioInsightProperties;
import com.lordkay.embabel.mcp.config.McpContextProperties;
import com.lordkay.embabel.mcp.format.BioInsightMarkdown;
import com.lordkay.embabel.mcp.planning.InvestigationIntent;
import com.lordkay.embabel.mcp.planning.InvestigationPlanner;

/** M1: investigation planning — always exposed (all tool profiles). */
@Component
public class BioInsightPlanningMcpTools extends BioInsightToolSupport {

    public BioInsightPlanningMcpTools(
            BioInsightApiClient api, BioInsightProperties properties, McpContextProperties context) {
        super(api, properties, context);
    }

    @McpTool(
            name = "plan_investigation",
            description =
                    "JSON plan (intent, entities, tool sequence, stop rules) before calling dossier or search tools.")
    public String planInvestigation(
            @McpToolParam(description = "Natural language research question", required = true) String question,
            @McpToolParam(
                            description =
                                    "Optional override: GENE_TARGET, DISEASE_TARGETS, COMPARE_GENES, LITERATURE, GENERAL",
                            required = false)
                    String intent,
            @McpToolParam(description = "markdown or json (default json)", required = false) String format) {
        InvestigationIntent routed = parseIntent(intent);
        String json = InvestigationPlanner.buildPlanJson(question, routed);
        if (wantsMarkdown(format)) {
            return respond(planAsMarkdown(json), "markdown");
        }
        return respond(json, "json", true);
    }

    private static InvestigationIntent parseIntent(String intent) {
        if (intent == null || intent.isBlank()) {
            return null;
        }
        try {
            return InvestigationIntent.valueOf(intent.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String planAsMarkdown(String json) {
        return "## Investigation plan\n\n" + BioInsightMarkdown.format(json) + "\n";
    }
}
