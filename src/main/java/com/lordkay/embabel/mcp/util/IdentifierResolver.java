package com.lordkay.embabel.mcp.util;

import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lordkay.embabel.mcp.client.BioInsightApiClient;

/**
 * Resolves gene symbols and disease names to canonical graph ids (M3).
 * Delegates to BioInsight {@code GET /resolve} for ontology-aware resolution.
 */
public final class IdentifierResolver {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private IdentifierResolver() {}

    public static String resolveGene(BioInsightApiClient api, String query) {
        return resolve(api, query, EntityType.GENE);
    }

    public static String resolveDisease(BioInsightApiClient api, String query) {
        return resolve(api, query, EntityType.DISEASE);
    }

    public static String resolve(BioInsightApiClient api, String query, EntityType entityType) {
        String q = query == null ? "" : query.trim();
        if (q.isEmpty()) {
            return error("Query is required.");
        }

        if (entityType == EntityType.GENE && looksLikeEnsemblGeneId(q)) {
            return resolvedGeneId(q);
        }
        if (entityType == EntityType.DISEASE && looksLikeOntologyDiseaseId(q)) {
            return resolvedDiseaseId(q);
        }

        String json =
                api.get(
                        "/resolve",
                        Map.of(
                                "query", q,
                                "entity_type", entityType.name().toLowerCase(Locale.ROOT)));

        if (json.contains("\"error\":true")) {
            return json;
        }

        try {
            JsonNode root = MAPPER.readTree(json);
            if (root.has("error") && root.get("error").asBoolean()) {
                return json;
            }
            return root.toString();
        } catch (Exception ex) {
            return error("Failed to parse resolve response: " + ex.getMessage());
        }
    }

    private static String resolvedGeneId(String geneId) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("entity_type", "gene");
        root.put("canonical_id", geneId.toUpperCase(Locale.ROOT));
        root.put("id_system", "ENSG");
        root.put("symbol", geneId.toUpperCase(Locale.ROOT));
        root.put("ambiguous", false);
        root.put("resolution_note", "Input is already an Ensembl gene id.");
        return root.toString();
    }

    private static String resolvedDiseaseId(String diseaseId) {
        String canonical = diseaseId.toUpperCase(Locale.ROOT);
        String idSystem =
                canonical.startsWith("EFO_")
                        ? "EFO"
                        : canonical.startsWith("MONDO_") ? "MONDO" : "disease_id";
        ObjectNode root = MAPPER.createObjectNode();
        root.put("entity_type", "disease");
        root.put("canonical_id", canonical);
        root.put("id_system", idSystem);
        root.put("name", canonical);
        root.put("ambiguous", false);
        root.put("resolution_note", "Input is already a disease ontology id.");
        return root.toString();
    }

    private static String error(String detail) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("error", true);
        root.put("detail", detail);
        return root.toString();
    }

    private static boolean looksLikeEnsemblGeneId(String q) {
        return q.matches("ENSG\\d{11}");
    }

    private static boolean looksLikeOntologyDiseaseId(String q) {
        return q.matches("(EFO|MONDO)_[0-9]+");
    }

    public enum EntityType {
        GENE,
        DISEASE
    }
}
