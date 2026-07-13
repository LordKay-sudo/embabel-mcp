package com.lordkay.embabel.mcp.tools;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.lordkay.embabel.mcp.client.BioInsightApiClient;
import com.lordkay.embabel.mcp.config.BioInsightProperties;
import com.lordkay.embabel.mcp.config.McpContextProperties;
import com.lordkay.embabel.mcp.format.BioInsightMarkdown;
import com.lordkay.embabel.mcp.planning.GapInvestigationPlanner;

/**
 * GapForge MCP tools — propose-only; L2 hypotheses require UI HITL approval.
 */
@Component
public class BioInsightGapMcpTools extends BioInsightToolSupport {

    public static final String COU =
            "Generate literature-backed gap hypotheses for scientific discussion; "
                    + "not for clinical care or regulatory submission.";

    public BioInsightGapMcpTools(
            BioInsightApiClient api, BioInsightProperties properties, McpContextProperties context) {
        super(api, properties, context);
    }

    @McpTool(
            name = "plan_gap_investigation",
            description =
                    "JSON plan for a stalled-program gap investigation (COU, risk tier L2, tool sequence, stop rules).")
    public String planGapInvestigation(
            @McpToolParam(description = "Research question about a stalled program", required = true)
                    String question,
            @McpToolParam(description = "Optional program id (e.g. prog-flurizan-ad)", required = false)
                    String programId,
            @McpToolParam(description = "markdown or json (default json)", required = false) String format) {
        String json = GapInvestigationPlanner.buildPlanJson(question, programId);
        if (wantsMarkdown(format)) {
            return respond("## Gap investigation plan\n\n" + BioInsightMarkdown.format(json) + "\n\n**COU:** " + COU, "markdown");
        }
        return respond(json, "json", true);
    }

    @McpTool(
            name = "build_program_dossier",
            description = "Fetch GapForge program dossier (trials, genes, gaps) from BioInsight API.")
    public String buildProgramDossier(
            @McpToolParam(description = "Program id", required = true) String programId,
            @McpToolParam(description = "markdown or json", required = false) String format) {
        String json = api.get("/programs/" + programId + "/dossier");
        if (wantsMarkdown(format)) {
            String ui = properties.getWebUiBaseUrl().replaceAll("/$", "") + "/program/" + programId;
            return respond(
                    "# Program dossier\n\n"
                            + BioInsightMarkdown.format(json)
                            + "\n\n**Verify in UI:** "
                            + ui
                            + "\n\n**COU:** "
                            + COU
                            + "\n",
                    "markdown");
        }
        return respond(json, "json", true);
    }

    @McpTool(
            name = "propose_gap_hypotheses",
            description =
                    "Create or list L2 gap hypotheses. New proposals always status=needs_review (HITL required).")
    public String proposeGapHypotheses(
            @McpToolParam(description = "Program id", required = true) String programId,
            @McpToolParam(
                            description =
                                    "If true, POST a new hypothesis; if false, list existing gaps for the program",
                            required = false)
                    Boolean create,
            @McpToolParam(description = "gap_class when create=true", required = false) String gapClass,
            @McpToolParam(description = "claim when create=true", required = false) String claim,
            @McpToolParam(description = "confidence 0-1 when create=true", required = false) Double confidence,
            @McpToolParam(description = "suggested_experiment when create=true", required = false)
                    String suggestedExperiment,
            @McpToolParam(description = "markdown or json", required = false) String format) {
        boolean doCreate = Boolean.TRUE.equals(create);
        if (doCreate) {
            if (gapClass == null || gapClass.isBlank() || claim == null || claim.isBlank()) {
                return respond(
                        "{\"error\":true,\"detail\":\"create=true requires gap_class and claim\"}",
                        "json",
                        true);
            }
            double conf = confidence == null ? 0.5 : confidence;
            String body =
                    "{"
                            + "\"program_id\":\""
                            + escape(programId)
                            + "\","
                            + "\"gap_class\":\""
                            + escape(gapClass)
                            + "\","
                            + "\"claim\":\""
                            + escape(claim)
                            + "\","
                            + "\"confidence\":"
                            + conf
                            + ","
                            + "\"suggested_experiment\":"
                            + (suggestedExperiment == null
                                    ? "null"
                                    : "\"" + escape(suggestedExperiment) + "\"")
                            + ","
                            + "\"insufficient_evidence\":true,"
                            + "\"literature_refs\":[]"
                            + "}";
            String json = api.postJson("/gaps/propose", body);
            return finishPropose(json, format);
        }
        String json = api.get("/gaps", java.util.Map.of("program_id", programId));
        return finishPropose(json, format);
    }

    @McpTool(
            name = "run_critic",
            description =
                    "Run adversarial critic on a gap hypothesis (clamps confidence; leaves status needs_review).")
    public String runCritic(
            @McpToolParam(description = "Gap hypothesis id", required = true) String gapId,
            @McpToolParam(description = "Optional counter-evidence note", required = false)
                    String extraCounterEvidence,
            @McpToolParam(description = "markdown or json", required = false) String format) {
        String body =
                extraCounterEvidence == null || extraCounterEvidence.isBlank()
                        ? "{}"
                        : "{\"extra_counter_evidence\":\"" + escape(extraCounterEvidence) + "\"}";
        String json = api.postJson("/gaps/" + gapId + "/critic", body);
        if (wantsMarkdown(format)) {
            return respond(
                    "## Critic result\n\n"
                            + BioInsightMarkdown.format(json)
                            + "\n\n**Next:** human review at /gaps/review\n\n**COU:** "
                            + COU
                            + "\n",
                    "markdown");
        }
        return respond(json, "json", true);
    }

    @McpTool(
            name = "export_review_bundle",
            description =
                    "Export GapForge provenance/review bundle. team_conclusions only includes approved cards.")
    public String exportReviewBundle(
            @McpToolParam(description = "Program id", required = false) String programId,
            @McpToolParam(description = "Gap id", required = false) String gapId,
            @McpToolParam(description = "markdown or json", required = false) String format) {
        java.util.Map<String, String> q = new java.util.LinkedHashMap<>();
        if (programId != null && !programId.isBlank()) {
            q.put("program_id", programId);
        }
        if (gapId != null && !gapId.isBlank()) {
            q.put("gap_id", gapId);
        }
        if (q.isEmpty()) {
            return respond(
                    "{\"error\":true,\"detail\":\"Provide program_id and/or gap_id\"}", "json", true);
        }
        String json = api.get("/export/review-bundle", q);
        if (wantsMarkdown(format)) {
            return respond(
                    "## Review bundle\n\n"
                            + BioInsightMarkdown.format(json)
                            + "\n\n**COU:** "
                            + COU
                            + "\n",
                    "markdown");
        }
        return respond(json, "json", true);
    }

    private String finishPropose(String json, String format) {
        if (wantsMarkdown(format)) {
            String ui = properties.getWebUiBaseUrl().replaceAll("/$", "") + "/gaps/review";
            return respond(
                    "# Gap hypotheses (L2 — HITL required)\n\n"
                            + BioInsightMarkdown.format(json)
                            + "\n\n**Review queue:** "
                            + ui
                            + "\n\n**COU:** "
                            + COU
                            + "\n",
                    "markdown");
        }
        return respond(json, "json", true);
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
