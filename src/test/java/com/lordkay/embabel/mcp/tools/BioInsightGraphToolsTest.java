package com.lordkay.embabel.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class BioInsightGraphToolsTest {

    @Test
    void extractFirstGeneId_parsesSearchResponse() {
        String json =
                """
                [{"id":"ENSG00000012048","symbol":"BRCA1","name":"BRCA1 DNA repair"}]
                """;
        assertEquals("ENSG00000012048", BioInsightGraphTools.extractFirstGeneId(json));
    }

    @Test
    void extractFirstGeneId_returnsNullWhenEmpty() {
        assertNull(BioInsightGraphTools.extractFirstGeneId("[]"));
    }
}
