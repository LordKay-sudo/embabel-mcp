package com.lordkay.embabel.mcp.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lordkay.embabel.mcp.client.BioInsightApiClient;

/**
 * Resolves gene symbols and disease names to canonical graph ids (M3).
 * Uses BioInsight search endpoints until dedicated resolve API ships in 2.x.
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
            return resolvedGene(q, q, List.of(), "Input is already an Ensembl gene id.");
        }
        if (entityType == EntityType.DISEASE && looksLikeOntologyDiseaseId(q)) {
            return resolvedDisease(q, q, List.of(), "Input is already a disease ontology id.");
        }

        String path = entityType == EntityType.GENE ? "/genes" : "/diseases";
        String searchJson = api.get(path, java.util.Map.of("q", q));
        if (searchJson.contains("\"error\":true")) {
            return searchJson;
        }

        try {
            JsonNode arr = MAPPER.readTree(searchJson);
            if (!arr.isArray() || arr.isEmpty()) {
                return notFound(entityType, q);
            }
            List<Candidate> candidates = parseCandidates(arr, entityType);
            if (entityType == EntityType.GENE) {
                return buildGeneResolution(q, candidates);
            }
            return buildDiseaseResolution(q, candidates);
        } catch (Exception ex) {
            return error("Failed to parse search response: " + ex.getMessage());
        }
    }

    private static String buildGeneResolution(String query, List<Candidate> candidates) {
        Candidate best = pickBest(query, candidates);
        List<Candidate> ambiguous = ambiguityList(query, candidates, best);
        String note =
                ambiguous.isEmpty()
                        ? "Single best match from gene search."
                        : "Multiple matches — confirm symbol before dossier.";
        return resolvedGene(best.id(), best.label(), ambiguous, note);
    }

    private static String buildDiseaseResolution(String query, List<Candidate> candidates) {
        Candidate best = pickBest(query, candidates);
        List<Candidate> ambiguous = ambiguityList(query, candidates, best);
        String note =
                ambiguous.isEmpty()
                        ? "Single best match from disease search."
                        : "Multiple disease matches — confirm id before get_disease_genes.";
        return resolvedDisease(best.id(), best.label(), ambiguous, note);
    }

    private static Candidate pickBest(String query, List<Candidate> candidates) {
        String upper = query.toUpperCase(Locale.ROOT);
        for (Candidate c : candidates) {
            if (c.label().equalsIgnoreCase(query) || c.id().equalsIgnoreCase(query)) {
                return c;
            }
        }
        for (Candidate c : candidates) {
            if (c.label().equalsIgnoreCase(upper)) {
                return c;
            }
        }
        return candidates.get(0);
    }

    private static List<Candidate> ambiguityList(String query, List<Candidate> all, Candidate best) {
        if (all.size() <= 1) {
            return List.of();
        }
        List<Candidate> others = new ArrayList<>();
        for (Candidate c : all) {
            if (!c.id().equals(best.id())) {
                others.add(c);
            }
        }
        if (best.label().equalsIgnoreCase(query) && others.size() == all.size() - 1) {
            return others;
        }
        return others.size() > 4 ? others.subList(0, 4) : others;
    }

    private static List<Candidate> parseCandidates(JsonNode arr, EntityType type) {
        List<Candidate> out = new ArrayList<>();
        for (JsonNode n : arr) {
            String id = text(n, "id");
            String label = type == EntityType.GENE ? text(n, "symbol") : text(n, "name");
            if (label == null || label.isBlank()) {
                label = text(n, "name");
            }
            out.add(new Candidate(id, label));
        }
        return out;
    }

    private static String resolvedGene(String canonicalId, String symbol, List<Candidate> ambiguous, String note) {
        ObjectNode root = baseResolution("gene", canonicalId, "ENSG", note);
        root.put("symbol", symbol);
        appendAmbiguity(root, ambiguous);
        return root.toString();
    }

    private static String resolvedDisease(String canonicalId, String name, List<Candidate> ambiguous, String note) {
        String idSystem = canonicalId.startsWith("EFO_") ? "EFO" : canonicalId.startsWith("MONDO_") ? "MONDO" : "disease_id";
        ObjectNode root = baseResolution("disease", canonicalId, idSystem, note);
        root.put("name", name);
        appendAmbiguity(root, ambiguous);
        return root.toString();
    }

    private static ObjectNode baseResolution(String entityType, String canonicalId, String idSystem, String note) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("entity_type", entityType);
        root.put("canonical_id", canonicalId);
        root.put("id_system", idSystem);
        root.put("ambiguous", false);
        root.put("resolution_note", note);
        return root;
    }

    private static void appendAmbiguity(ObjectNode root, List<Candidate> ambiguous) {
        if (!ambiguous.isEmpty()) {
            root.put("ambiguous", true);
            ArrayNode arr = root.putArray("candidates");
            for (Candidate c : ambiguous) {
                ObjectNode o = arr.addObject();
                o.put("id", c.id());
                o.put("label", c.label());
            }
        }
    }

    private static String notFound(EntityType type, String query) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("error", true);
        root.put("entity_type", type.name().toLowerCase(Locale.ROOT));
        root.put("detail", "No " + type.name().toLowerCase(Locale.ROOT) + " found for: " + query);
        root.put("suggestion", "Try a different spelling or use search_" + (type == EntityType.GENE ? "genes" : "diseases"));
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

    private static String text(JsonNode n, String field) {
        return n.has(field) && !n.get(field).isNull() ? n.get(field).asText() : "";
    }

    public enum EntityType {
        GENE,
        DISEASE
    }

    private record Candidate(String id, String label) {}
}
