package com.lordkay.embabel.mcp.format;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BioInsightMarkdownTest {

    @Test
    void formatsStatsAsTable() {
        String md = BioInsightMarkdown.format("{\"genes\":30,\"diseases\":12,\"proteins\":10,\"associations\":105}");
        assertTrue(md.contains("## Graph statistics"));
        assertTrue(md.contains("| Genes | 30 |"));
    }

    @Test
    void formatsCompareWithOverlap() {
        String json =
                """
                {"symbols":["BRCA1","TP53"],"genes":[{"gene_id":"E1","symbol":"BRCA1","name":null,"disease_count":2,"top_diseases":[{"disease_id":"D1","name":"breast cancer","score":0.9}]}],"overlapping_disease_names":["breast cancer"]}
                """;
        String md = BioInsightMarkdown.format(json);
        assertTrue(md.contains("## Gene comparison"));
        assertTrue(md.contains("Overlapping"));
    }
}
