package com.lordkay.embabel.mcp.util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lordkay.embabel.mcp.client.BioInsightApiClient;

/** M7: auditable JSON bundle for HITL handoff. */
public final class ProvenanceBundleExporter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ProvenanceBundleExporter() {}

    public static String export(BioInsightApiClient api, String webUiBaseUrl, String geneIdOrSymbol) {
        String resolved = IdentifierResolver.resolveGene(api, geneIdOrSymbol);
        if (resolved.contains("\"error\":true")) {
            return resolved;
        }
        try {
            JsonNode r = MAPPER.readTree(resolved);
            String geneId = r.get("canonical_id").asText();
            String symbol = r.has("symbol") ? r.get("symbol").asText() : geneIdOrSymbol;

            ObjectNode bundle = MAPPER.createObjectNode();
            bundle.put("exported_at", Instant.now().toString());
            bundle.put("gene_id", geneId);
            bundle.put("symbol", symbol);
            bundle.set("identifier_resolution", r);

            String meta = api.get("/meta");
            bundle.set("meta", parseOrError(meta));

            List<String> queries = new ArrayList<>();
            queries.add("GET /meta");
            queries.add("GET /genes/" + geneId);
            queries.add("GET /genes/" + geneId + "/external-links");
            queries.add("GET /genes/" + geneId + "/evidence?limit=5");

            String detail = api.get("/genes/" + geneId);
            bundle.set("gene_detail", parseOrError(detail));
            queries.add("GET /genes/" + geneId);

            String links = api.get("/genes/" + geneId + "/external-links");
            bundle.set("external_links", parseOrError(links));

            String evidence = TargetEvidenceFetcher.fetch(api, geneId, null, 5);
            bundle.set("evidence_sample", parseOrError(evidence));

            ArrayNode q = bundle.putArray("queries_run");
            queries.forEach(q::add);

            String base = webUiBaseUrl == null ? "http://localhost:8080" : webUiBaseUrl.replaceAll("/$", "");
            bundle.put("verify_ui_url", base + "/gene/" + geneId);
            bundle.put("api_docs", "http://localhost:8000/docs");

            return bundle.toString();
        } catch (Exception ex) {
            ObjectNode err = MAPPER.createObjectNode();
            err.put("error", true);
            err.put("detail", "Failed to build provenance bundle: " + ex.getMessage());
            return err.toString();
        }
    }

    private static JsonNode parseOrError(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception ex) {
            ObjectNode n = MAPPER.createObjectNode();
            n.put("raw", json);
            return n;
        }
    }
}
