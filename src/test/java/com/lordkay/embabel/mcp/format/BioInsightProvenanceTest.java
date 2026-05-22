package com.lordkay.embabel.mcp.format;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BioInsightProvenanceTest {

    @Test
    void footerFromMetaJson_includesVersionAndUiLink() {
        String meta =
                """
                {"data_version":"demo-v1","release_date":"2024-06-01",\
                "associations_are_correlative":true,"disclaimer":"Not for clinical use."}
                """;
        String footer = BioInsightProvenance.footerFromMetaJson(meta, "http://localhost:8080", "ENSG1");
        assertTrue(footer.contains("demo-v1"));
        assertTrue(footer.contains("correlative"));
        assertTrue(footer.contains("http://localhost:8080/gene/ENSG1"));
    }

    @Test
    void footerFromMetaJson_fallsBackOnError() {
        String footer = BioInsightProvenance.footerFromMetaJson("{\"error\":true}", "http://localhost:8080", null);
        assertTrue(footer.contains("Demo/sample"));
    }
}
