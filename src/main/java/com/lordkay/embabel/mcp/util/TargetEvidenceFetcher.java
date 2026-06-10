package com.lordkay.embabel.mcp.util;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lordkay.embabel.mcp.client.BioInsightApiClient;

/**
 * Fetches target–disease evidence (M4). Prefers {@code GET /genes/{id}/evidence} when available;
 * falls back to ranked associations with score-only placeholders.
 */
public final class TargetEvidenceFetcher {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TargetEvidenceFetcher() {}

    public static String fetch(
            BioInsightApiClient api, String geneIdOrSymbol, String diseaseId, int limit) {
        String geneId = resolveGeneId(api, geneIdOrSymbol);
        if (geneId == null) {
            return notFound("Gene not found: " + geneIdOrSymbol);
        }

        if (diseaseId != null && !diseaseId.isBlank()) {
            String dedicated = tryDedicatedEvidence(api, geneId, diseaseId);
            if (dedicated != null) {
                return dedicated;
            }
            return fromAssociations(api, geneId, diseaseId, limit, true);
        }

        String allEvidence = tryDedicatedEvidence(api, geneId, null);
        if (allEvidence != null) {
            return allEvidence;
        }
        return fromAssociations(api, geneId, null, limit, false);
    }

    private static String tryDedicatedEvidence(BioInsightApiClient api, String geneId, String diseaseId) {
        String path = "/genes/" + geneId + "/evidence";
        Map<String, String> params =
                diseaseId != null && !diseaseId.isBlank()
                        ? Map.of("disease_id", diseaseId)
                        : Map.of();
        String json = params.isEmpty() ? api.get(path) : api.get(path, params);
        if (json.contains("\"error\":true")) {
            return null;
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            if (root.has("evidence") || root.has("evidence_breakdown")) {
                ObjectNode out = MAPPER.createObjectNode();
                out.put("gene_id", geneId);
                if (diseaseId != null) {
                    out.put("disease_id", diseaseId);
                }
                out.put("evidence_breakdown_available", true);
                out.set("payload", root);
                return out.toString();
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }

    private static String fromAssociations(
            BioInsightApiClient api, String geneId, String diseaseId, int limit, boolean singleDisease) {
        String diseasesJson = api.get("/genes/" + geneId + "/diseases", Map.of("limit", String.valueOf(limit)));
        if (diseasesJson.contains("\"error\":true")) {
            return diseasesJson;
        }
        try {
            JsonNode root = MAPPER.readTree(diseasesJson);
            ObjectNode out = MAPPER.createObjectNode();
            out.put("gene_id", geneId);
            out.put("symbol", text(root, "symbol"));
            out.put("evidence_breakdown_available", false);
            out.put(
                    "note",
                    "BioInsight 1.4 typed evidence not on API yet — showing association scores as provisional evidence.");
            ArrayNode items = out.putArray("associations");
            for (JsonNode d : root.get("diseases")) {
                if (singleDisease && diseaseId != null && !diseaseId.equals(text(d, "disease_id"))) {
                    continue;
                }
                ObjectNode assoc = items.addObject();
                assoc.put("disease_id", text(d, "disease_id"));
                assoc.put("disease_name", text(d, "name"));
                assoc.put("score", d.get("score").asDouble());
                ArrayNode evidence = assoc.putArray("evidence");
                ObjectNode row = evidence.addObject();
                row.put("type", "association_score");
                row.put("source", "opentargets-sample");
                row.put("score", d.get("score").asDouble());
                if (d.has("evidence") && d.get("evidence").isArray()) {
                    out.put("evidence_breakdown_available", true);
                    assoc.set("evidence", d.get("evidence"));
                }
            }
            if (singleDisease && items.isEmpty()) {
                return notFound("No association for gene " + geneId + " and disease " + diseaseId);
            }
            return out.toString();
        } catch (Exception ex) {
            return error("Failed to parse diseases response: " + ex.getMessage());
        }
    }

    private static String resolveGeneId(BioInsightApiClient api, String geneIdOrSymbol) {
        if (geneIdOrSymbol.matches("ENSG\\d{11}")) {
            return geneIdOrSymbol;
        }
        String resolved = IdentifierResolver.resolveGene(api, geneIdOrSymbol);
        if (resolved.contains("\"error\":true")) {
            return null;
        }
        try {
            return MAPPER.readTree(resolved).get("canonical_id").asText();
        } catch (Exception ex) {
            return GeneIdParser.extractFirstGeneId(api.get("/genes", Map.of("q", geneIdOrSymbol)));
        }
    }

    private static String notFound(String detail) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("error", true);
        root.put("detail", detail);
        return root.toString();
    }

    private static String error(String detail) {
        return notFound(detail);
    }

    private static String text(JsonNode n, String field) {
        return n.has(field) && !n.get(field).isNull() ? n.get(field).asText() : "";
    }
}
