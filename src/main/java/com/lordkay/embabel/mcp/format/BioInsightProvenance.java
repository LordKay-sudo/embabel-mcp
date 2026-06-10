package com.lordkay.embabel.mcp.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lordkay.embabel.mcp.client.BioInsightApiClient;

/**
 * Appends dataset provenance from {@code GET /meta}, with a static fallback if the API is unreachable.
 */
public final class BioInsightProvenance {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String FALLBACK_DISCLAIMER =
            "Demo/sample Open Targets–style data. Associations are correlative, not causal. Not for clinical use.";

    private BioInsightProvenance() {}

    public static String footer(BioInsightApiClient api, String webUiBaseUrl, String geneId) {
        return footerFromMetaJson(api.get("/meta"), webUiBaseUrl, geneId);
    }

    static String footerFromMetaJson(String metaJson, String webUiBaseUrl, String geneId) {
        StringBuilder sb = new StringBuilder("---\n\n### Provenance\n\n");
        if (metaJson != null && !metaJson.contains("\"error\":true")) {
            try {
                JsonNode m = MAPPER.readTree(metaJson);
                sb.append("- **Data version:** `").append(text(m, "data_version")).append("`\n");
                sb.append("- **Release date:** ").append(text(m, "release_date")).append("\n");
                if (m.has("associations_are_correlative") && m.get("associations_are_correlative").asBoolean()) {
                    sb.append("- **Note:** associations are correlative, not causal\n");
                }
                if (m.has("sources") && m.get("sources").isArray() && !m.get("sources").isEmpty()) {
                    sb.append("- **Sources:**\n");
                    for (JsonNode s : m.get("sources")) {
                        String name = text(s, "name");
                        String url = text(s, "url");
                        if (!url.equals("—")) {
                            sb.append("  - [").append(name).append("](").append(url).append(")\n");
                        } else {
                            sb.append("  - ").append(name).append("\n");
                        }
                    }
                }
                sb.append("- **Disclaimer:** ").append(text(m, "disclaimer")).append("\n");
            } catch (Exception ignored) {
                sb.append("- ").append(FALLBACK_DISCLAIMER).append("\n");
            }
        } else {
            sb.append("- ").append(FALLBACK_DISCLAIMER).append("\n");
        }
        if (geneId != null && !geneId.isBlank()) {
            String base = webUiBaseUrl == null ? "http://localhost:8080" : webUiBaseUrl.replaceAll("/$", "");
            sb.append("- **Verify in UI:** [")
                    .append(geneId)
                    .append("](")
                    .append(base)
                    .append("/gene/")
                    .append(geneId)
                    .append(")\n");
        }
        return sb.toString();
    }

    private static String text(JsonNode n, String field) {
        return n.has(field) && !n.get(field).isNull() ? n.get(field).asText() : "—";
    }
}
