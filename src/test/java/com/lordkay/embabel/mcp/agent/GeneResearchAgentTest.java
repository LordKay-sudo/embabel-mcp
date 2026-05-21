package com.lordkay.embabel.mcp.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GeneResearchAgentTest {

    @Test
    void extractSymbol_fromPlainSymbol() {
        assertEquals("BRCA1", GeneResearchAgent.extractSymbol("BRCA1"));
    }

    @Test
    void extractSymbol_fromSentence() {
        assertEquals("TP53", GeneResearchAgent.extractSymbol("Please research gene TP53 for me"));
    }

    @Test
    void extractSymbol_rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> GeneResearchAgent.extractSymbol("  "));
    }
}
