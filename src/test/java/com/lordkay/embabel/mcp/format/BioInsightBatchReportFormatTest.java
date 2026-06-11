package com.lordkay.embabel.mcp.format;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BioInsightBatchReportFormatTest {

    @Test
    void formatsBatchLookup() {
        String json =
                """
                {"hits":[{"query":"BRCA1","gene_id":"ENSG00000012048","symbol":"BRCA1","name":"BRCA1","disease_count":4}],
                "unresolved":["NOPE"]}
                """;
        String md = BioInsightMarkdown.format(json);
        assertTrue(md.contains("Batch gene lookup"));
        assertTrue(md.contains("BRCA1"));
        assertTrue(md.contains("Unresolved"));
        assertTrue(md.contains("NOPE"));
    }

    @Test
    void formatsGeneReport() {
        String json =
                """
                {"gene_id":"ENSG00000012048","symbol":"BRCA1",
                "provenance":{"data_version":"demo-v2","release_date":"2024-06-01"},
                "columns":[],
                "associations":[{"disease_name":"breast cancer","score":0.92,"source":"opentargets","evidence_types":"genetic_association"}]}
                """;
        String md = BioInsightMarkdown.format(json);
        assertTrue(md.contains("Gene report"));
        assertTrue(md.contains("demo-v2"));
        assertTrue(md.contains("breast cancer"));
        assertTrue(md.contains("genetic_association"));
    }
}
