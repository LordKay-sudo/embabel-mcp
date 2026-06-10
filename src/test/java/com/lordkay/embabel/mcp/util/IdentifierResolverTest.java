package com.lordkay.embabel.mcp.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.lordkay.embabel.mcp.client.BioInsightApiClient;
import com.lordkay.embabel.mcp.config.BioInsightProperties;

class IdentifierResolverTest {

    @Test
    void resolveGene_parsesSearchHits() {
        BioInsightProperties props = new BioInsightProperties();
        props.setApiBaseUrl("http://example.test/api/v1");
        BioInsightApiClient api = new BioInsightApiClient(props) {
            @Override
            public String get(String path, java.util.Map<String, String> queryParams) {
                return """
                        [{"id":"ENSG00000012048","symbol":"BRCA1","name":"BRCA1 DNA repair"}]
                        """;
            }
        };
        String json = IdentifierResolver.resolveGene(api, "BRCA1");
        assertTrue(json.contains("\"canonical_id\":\"ENSG00000012048\""));
        assertFalse(json.contains("\"ambiguous\":true"));
    }

    @Test
    void resolveGene_acceptsEnsemblIdDirectly() {
        BioInsightProperties props = new BioInsightProperties();
        props.setApiBaseUrl("http://example.test/api/v1");
        BioInsightApiClient api = new BioInsightApiClient(props);
        String json = IdentifierResolver.resolveGene(api, "ENSG00000012048");
        assertTrue(json.contains("\"canonical_id\":\"ENSG00000012048\""));
        assertTrue(json.contains("Ensembl"));
    }
}
