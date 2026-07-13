package com.lordkay.embabel.mcp.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Export;
import com.embabel.agent.core.hitl.WaitFor;
import com.embabel.agent.domain.io.UserInput;
import com.lordkay.embabel.mcp.config.BioInsightHitlProperties;
import com.lordkay.embabel.mcp.domain.GapResearchApproval;
import com.lordkay.embabel.mcp.domain.GapResearchReport;
import com.lordkay.embabel.mcp.domain.ProgramGapBundle;
import com.lordkay.embabel.mcp.tools.BioInsightGapMcpTools;

/**
 * GapForge agent: load program dossier → optional HITL acknowledgment → markdown report.
 * Does not approve individual L2 hypothesis cards; UI review remains ground truth.
 */
@Agent(
        name = "GapResearchAgent",
        description = "Investigate a stalled drug program and produce a GapForge dossier report",
        beanName = "gapResearchAgent")
public class GapResearchAgent {

    private final BioInsightGapMcpTools gapTools;
    private final BioInsightHitlProperties hitl;

    public GapResearchAgent(BioInsightGapMcpTools gapTools, BioInsightHitlProperties hitl) {
        this.gapTools = gapTools;
        this.hitl = hitl;
    }

    @Action
    public ProgramGapBundle loadProgram(UserInput input) {
        String text = input.getContent() == null ? "" : input.getContent().trim();
        String programId = extractProgramId(text);
        if (programId == null) {
            programId = "prog-flurizan-ad";
        }
        String plan = gapTools.planGapInvestigation("Investigate " + programId, programId, "json");
        String dossier = gapTools.buildProgramDossier(programId, "json");
        String gaps = gapTools.proposeGapHypotheses(programId, false, null, null, null, null, "json");
        return new ProgramGapBundle(programId, plan, dossier, gaps);
    }

    @Action(description = "Acknowledge GapForge dossier (does not approve L2 gap cards)")
    public GapResearchApproval requestHumanReview(ProgramGapBundle bundle) {
        if (!hitl.isEnabled()) {
            return new GapResearchApproval(
                    true,
                    "Auto-acknowledged (MCP/server mode). Individual gap cards remain needs_review until UI HITL.");
        }
        return WaitFor.formSubmission(
                """
                ## GapForge review acknowledgment: %s

                Open BioInsight: http://localhost:8080/program/%s
                Review queue: http://localhost:8080/gaps/review

                Acknowledging the dossier does **not** approve L2 hypothesis cards.
                """
                        .formatted(bundle.programId(), bundle.programId()),
                GapResearchApproval.class);
    }

    @AchievesGoal(
            description = "GapForge program investigation report",
            export =
                    @Export(
                            remote = true,
                            name = "research_program_gaps",
                            startingInputTypes = {UserInput.class}))
    @Action
    public GapResearchReport writeReport(ProgramGapBundle bundle, GapResearchApproval approval) {
        String criticHint = "";
        String gaps = bundle.gapsJson();
        int idIdx = gaps.indexOf("\"id\":\"");
        if (idIdx >= 0) {
            int start = idIdx + 6;
            int end = gaps.indexOf('"', start);
            if (end > start) {
                String gapId = gaps.substring(start, end);
                criticHint = gapTools.runCritic(gapId, null, "markdown");
            }
        }
        String body =
                "# GapForge report: **"
                        + bundle.programId()
                        + "**\n\n"
                        + "_Educational / research use only — not clinical advice._\n\n"
                        + "## Plan\n\n"
                        + bundle.planJson()
                        + "\n\n## Dossier\n\n"
                        + bundle.dossierJson()
                        + "\n\n## Hypotheses\n\n"
                        + bundle.gapsJson()
                        + "\n\n## Critic\n\n"
                        + criticHint
                        + "\n\n## HITL\n\n"
                        + "acknowledged="
                        + approval.approved()
                        + "; notes="
                        + approval.reviewerNotes()
                        + "\n\n**COU:** "
                        + BioInsightGapMcpTools.COU
                        + "\n\n**Next:** approve/reject cards at `/gaps/review`.\n";
        return new GapResearchReport(bundle.programId(), body, approval.approved());
    }

    private static String extractProgramId(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String[] tokens = text.split("\\s+");
        for (String t : tokens) {
            if (t.startsWith("prog-")) {
                return t.replaceAll("[^a-zA-Z0-9\\-]", "");
            }
        }
        if (text.toLowerCase().contains("flurizan")) {
            return "prog-flurizan-ad";
        }
        return null;
    }
}
