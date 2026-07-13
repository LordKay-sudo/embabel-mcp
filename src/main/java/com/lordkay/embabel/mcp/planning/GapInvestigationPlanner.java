package com.lordkay.embabel.mcp.planning;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Heuristic planner for GapForge stalled-program investigations. */
public final class GapInvestigationPlanner {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GapInvestigationPlanner() {}

    public static String buildPlanJson(String question, String programId) {
        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("intent", "GAP_INVESTIGATION");
        plan.put(
                "cou",
                "Generate literature-backed gap hypotheses for scientific discussion; "
                        + "not for clinical care or regulatory submission.");
        plan.put("risk_tier", "L2");
        plan.put("question", question == null ? "" : question);
        plan.put("program_id", programId);
        plan.put(
                "tool_sequence",
                List.of(
                        "plan_gap_investigation",
                        "build_program_dossier",
                        "propose_gap_hypotheses",
                        "run_critic",
                        "discern_artifact",
                        "export_review_bundle"));
        plan.put(
                "stop_rules",
                List.of(
                        "Do not approve L2 hypotheses via MCP — use web HITL review queue",
                        "Do not invent chemistry, doses, or patient advice (L3 blocked)",
                        "If dual-channel evidence missing, set insufficient_evidence=true",
                        "Always run critic and discern_artifact before asking a human to approve",
                        "If discern action=block, do not present as a team conclusion"));
        plan.put(
                "hitl",
                Map.of(
                        "required",
                        true,
                        "ui_path",
                        "/gaps/review",
                        "note",
                        "Agents propose; humans dispose."));
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(plan);
        } catch (JsonProcessingException e) {
            return "{\"error\":true,\"detail\":\"failed to serialize plan\"}";
        }
    }
}
