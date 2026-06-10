package com.lordkay.embabel.mcp.planning;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class InvestigationPlannerTest {

    @Test
    void routeIntent_detectsCompare() {
        assertEquals(
                InvestigationIntent.COMPARE_GENES,
                InvestigationPlanner.routeIntent("Compare BRCA1 vs TP53 disease overlap"));
    }

    @Test
    void routeIntent_detectsDiseaseFirst() {
        assertEquals(
                InvestigationIntent.DISEASE_TARGETS,
                InvestigationPlanner.routeIntent("What are the top targets for breast cancer?"));
    }

    @Test
    void routeIntent_detectsLiterature() {
        assertEquals(
                InvestigationIntent.LITERATURE,
                InvestigationPlanner.routeIntent("What does the literature say about BRCA1 mechanism?"));
    }

    @Test
    void buildPlanJson_includesToolSequenceAndStopRules() {
        String json = InvestigationPlanner.buildPlanJson("Summarize BRCA1 targets", null);
        assertTrue(json.contains("\"intent\":\"GENE_TARGET\""));
        assertTrue(json.contains("resolve_identifier"));
        assertTrue(json.contains("build_target_dossier"));
        assertTrue(json.contains("stop_rules"));
        assertTrue(json.contains("BRCA1"));
    }
}
