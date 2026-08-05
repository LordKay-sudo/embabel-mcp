package com.lordkay.embabel.mcp.planning;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GapInvestigationPlannerTest {

    @Test
    void buildPlanJson_includesRdfExportAfterReviewBundle() {
        String json = GapInvestigationPlanner.buildPlanJson("Why did Flurizan stall?", "prog-flurizan-ad");
        assertTrue(json.contains("export_review_bundle"));
        assertTrue(json.contains("export_approved_rdf"));
        assertTrue(json.indexOf("export_review_bundle") < json.indexOf("export_approved_rdf"));
    }
}
