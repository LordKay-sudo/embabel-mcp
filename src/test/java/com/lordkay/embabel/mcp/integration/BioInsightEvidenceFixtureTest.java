package com.lordkay.embabel.mcp.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import com.lordkay.embabel.mcp.format.BioInsightMarkdown;

/** M11: markdown formatting against frozen BioInsight evidence JSON (no live Neo4j). */
class BioInsightEvidenceFixtureTest {

    @Test
    void formatsFrozenEvidenceFixture() throws Exception {
        String json =
                new ClassPathResource("bioinsight-evidence-sample.json")
                        .getContentAsString(StandardCharsets.UTF_8);
        String md = BioInsightMarkdown.format(json);
        assertTrue(md.contains("genetic_association"));
        assertTrue(md.contains("breast cancer"));
        assertTrue(md.contains("BRCA1"));
    }
}
