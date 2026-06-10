package com.lordkay.embabel.mcp.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Converts BioInsight JSON API payloads into Markdown for LLM clients.
 */
public final class BioInsightMarkdown {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private BioInsightMarkdown() {}

    public static String format(String json) {
        if (json == null || json.isBlank()) {
            return "_No data_";
        }
        if (json.contains("\"error\":true")) {
            return "**Error**\n\n```json\n" + json + "\n```";
        }
        try {
            JsonNode root = MAPPER.readTree(json);
            if (root.isArray()) {
                if (root.size() > 0 && root.get(0).has("symbol")) {
                    return formatGeneSearch(root);
                }
                return formatDiseaseSearch(root);
            }
            if (root.has("genes") && root.has("symbols")) {
                return formatCompare(root);
            }
            if (root.has("genes") && root.has("disease_name")) {
                return formatDiseaseGenes(root);
            }
            if (root.has("diseases") && root.has("symbol")) {
                return formatGeneDiseases(root);
            }
            if (root.has("genes") && root.has("diseases") && root.has("proteins")) {
                return formatStats(root);
            }
            if (root.has("status") && root.has("neo4j")) {
                return formatHealth(root);
            }
            if (root.has("data_version") && root.has("disclaimer")) {
                return formatMeta(root);
            }
            if (root.has("symbol") && root.has("disease_count")) {
                return formatGeneDetail(root);
            }
            if (root.has("name") && root.has("gene_count")) {
                return formatDiseaseDetail(root);
            }
            if (root.has("nodes") && root.has("edges")) {
                return formatNeighbors(root);
            }
            if (root.has("detail") && root.has("neighbors")) {
                return formatInvestigation(root);
            }
            if (root.has("intent") && root.has("tool_sequence")) {
                return formatInvestigationPlan(root);
            }
            if (root.has("canonical_id") && root.has("entity_type")) {
                return formatIdentifierResolution(root);
            }
            if (root.has("associations") && root.has("evidence_breakdown_available")) {
                return formatTargetEvidence(root);
            }
            if (root.has("payload") && root.get("payload").has("evidence")) {
                return formatGeneEvidencePayload(root);
            }
            if (root.has("evidence") && root.get("evidence").isArray() && root.has("symbol")) {
                return formatGeneEvidenceResponse(root);
            }
            if (root.has("links") && root.has("gene_id")) {
                return formatExternalLinks(root);
            }
            if (root.has("exported_at") && root.has("queries_run")) {
                return formatProvenanceBundle(root);
            }
            return "```json\n" + MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root) + "\n```";
        } catch (Exception e) {
            return json;
        }
    }

    private static String formatHealth(JsonNode n) {
        return "## BioInsight health\n\n"
                + "- **Status:** " + n.get("status").asText() + "\n"
                + "- **Neo4j:** " + n.get("neo4j").asBoolean() + "\n";
    }

    private static String formatMeta(JsonNode n) {
        StringBuilder sb = new StringBuilder("## Dataset metadata\n\n");
        sb.append("- **Service:** ").append(cell(n, "service")).append("\n");
        sb.append("- **API version:** ").append(cell(n, "api_version")).append("\n");
        sb.append("- **Data version:** `").append(cell(n, "data_version")).append("`\n");
        sb.append("- **Release date:** ").append(cell(n, "release_date")).append("\n");
        if (n.has("associations_are_correlative")) {
            sb.append("- **Associations:** correlative (not causal)\n");
        }
        sb.append("\n**Disclaimer:** ").append(cell(n, "disclaimer")).append("\n");
        if (n.has("sources") && n.get("sources").isArray()) {
            sb.append("\n### Sources\n\n");
            for (JsonNode s : n.get("sources")) {
                sb.append("- [")
                        .append(cell(s, "name"))
                        .append("](")
                        .append(cell(s, "url"))
                        .append(")");
                if (s.has("license") && !s.get("license").isNull()) {
                    sb.append(" — ").append(s.get("license").asText());
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private static String formatStats(JsonNode n) {
        return "## Graph statistics\n\n"
                + "| Metric | Count |\n|--------|------:|\n"
                + "| Genes | " + n.get("genes").asInt() + " |\n"
                + "| Diseases | " + n.get("diseases").asInt() + " |\n"
                + "| Proteins | " + n.get("proteins").asInt() + " |\n"
                + "| Associations | " + n.get("associations").asInt() + " |\n";
    }

    private static String formatGeneSearch(JsonNode arr) {
        if (arr.isEmpty()) {
            return "_No genes matched._";
        }
        StringBuilder sb = new StringBuilder("## Gene search results\n\n");
        sb.append("| Symbol | ID | Name |\n|--------|----|------|\n");
        for (JsonNode g : arr) {
            sb.append("| ")
                    .append(cell(g, "symbol"))
                    .append(" | ")
                    .append(cell(g, "id"))
                    .append(" | ")
                    .append(cell(g, "name"))
                    .append(" |\n");
        }
        return sb.toString();
    }

    private static String formatDiseaseSearch(JsonNode arr) {
        if (arr.isEmpty()) {
            return "_No diseases matched._";
        }
        StringBuilder sb = new StringBuilder("## Disease search results\n\n");
        sb.append("| Name | ID |\n|------|----|\n");
        for (JsonNode d : arr) {
            sb.append("| ").append(cell(d, "name")).append(" | `").append(d.get("id").asText()).append("` |\n");
        }
        return sb.toString();
    }

    private static String formatDiseaseDetail(JsonNode n) {
        return "## Disease\n\n"
                + "- **Name:** " + n.get("name").asText() + "\n"
                + "- **ID:** `" + n.get("id").asText() + "`\n"
                + "- **Linked genes:** " + n.get("gene_count").asInt() + "\n";
    }

    private static String formatGeneDetail(JsonNode n) {
        return "## Gene\n\n"
                + "- **Symbol:** " + n.get("symbol").asText() + "\n"
                + "- **ID:** `" + n.get("id").asText() + "`\n"
                + "- **Name:** " + cell(n, "name") + "\n"
                + "- **Disease links:** " + n.get("disease_count").asInt() + "\n"
                + "- **Protein links:** " + n.get("protein_count").asInt() + "\n";
    }

    private static String formatDiseaseGenes(JsonNode n) {
        StringBuilder sb = new StringBuilder("## Targets for **");
        sb.append(n.get("disease_name").asText()).append("**\n\n");
        sb.append("_Min score: ").append(n.get("min_score").asDouble()).append("_\n\n");
        sb.append("| Rank | Symbol | Score | Gene ID |\n|-----:|--------|------:|---------|\n");
        int rank = 1;
        for (JsonNode g : n.get("genes")) {
            sb.append("| ")
                    .append(rank++)
                    .append(" | ")
                    .append(cell(g, "symbol"))
                    .append(" | ")
                    .append(String.format("%.3f", g.get("score").asDouble()))
                    .append(" | `")
                    .append(g.get("gene_id").asText())
                    .append("` |\n");
        }
        if (rank == 1) {
            return sb + "\n_No genes above threshold._\n";
        }
        return sb.toString();
    }

    private static String formatGeneDiseases(JsonNode n) {
        StringBuilder sb = new StringBuilder("## Diseases for **");
        sb.append(n.get("symbol").asText()).append("**\n\n");
        sb.append("_Min score: ").append(n.get("min_score").asDouble()).append("_\n\n");
        sb.append("| Rank | Disease | Score | ID |\n|-----:|---------|------:|----|\n");
        int rank = 1;
        for (JsonNode d : n.get("diseases")) {
            sb.append("| ")
                    .append(rank++)
                    .append(" | ")
                    .append(cell(d, "name"))
                    .append(" | ")
                    .append(String.format("%.3f", d.get("score").asDouble()))
                    .append(" | `")
                    .append(d.get("disease_id").asText())
                    .append("` |\n");
        }
        if (rank == 1) {
            return sb + "\n_No diseases above threshold._\n";
        }
        return sb.toString();
    }

    private static String formatCompare(JsonNode n) {
        StringBuilder sb = new StringBuilder("## Gene comparison: ");
        sb.append(String.join(", ", iterableText(n.get("symbols")))).append("\n\n");
        for (JsonNode g : n.get("genes")) {
            sb.append("### ").append(g.get("symbol").asText()).append("\n\n");
            sb.append("- **ID:** `").append(g.get("gene_id").asText()).append("`\n");
            sb.append("- **Total disease links:** ").append(g.get("disease_count").asInt()).append("\n\n");
            sb.append("| Disease | Score |\n|---------|------:|\n");
            for (JsonNode d : g.get("top_diseases")) {
                sb.append("| ")
                        .append(d.get("name").asText())
                        .append(" | ")
                        .append(String.format("%.3f", d.get("score").asDouble()))
                        .append(" |\n");
            }
            sb.append("\n");
        }
        JsonNode overlap = n.get("overlapping_disease_names");
        if (overlap != null && overlap.size() > 0) {
            sb.append("### Overlapping top diseases\n\n");
            for (JsonNode name : overlap) {
                sb.append("- ").append(name.asText()).append("\n");
            }
        } else {
            sb.append("_No overlapping diseases in top-N lists._\n");
        }
        return sb.toString();
    }

    private static String formatNeighbors(JsonNode n) {
        StringBuilder sb = new StringBuilder("## Neighborhood for `");
        sb.append(n.get("gene_id").asText()).append("`\n\n");
        sb.append("### Disease associations\n\n");
        sb.append("| Disease | Score |\n|---------|------:|\n");
        boolean any = false;
        for (JsonNode e : n.get("edges")) {
            if (!"ASSOCIATED_WITH".equals(e.get("type").asText())) {
                continue;
            }
            any = true;
            String diseaseId = e.get("target").asText();
            String name = findNodeName(n.get("nodes"), diseaseId);
            double score = e.has("score") && !e.get("score").isNull() ? e.get("score").asDouble() : 0;
            sb.append("| ").append(name).append(" | ").append(String.format("%.3f", score)).append(" |\n");
        }
        if (!any) {
            sb.append("_None_\n");
        }
        return sb.toString();
    }

    private static String formatInvestigation(JsonNode n) {
        return "## Investigation: " + n.get("symbol").asText() + "\n\n"
                + BioInsightMarkdown.format(n.get("detail").toString())
                + "\n"
                + BioInsightMarkdown.format(n.get("neighbors").toString());
    }

    private static String formatInvestigationPlan(JsonNode n) {
        StringBuilder sb = new StringBuilder("## Investigation plan\n\n");
        sb.append("- **Intent:** `").append(n.get("intent").asText()).append("`\n");
        sb.append("- **Question:** ").append(cell(n, "question")).append("\n");
        if (n.has("routing_note")) {
            sb.append("- **Routing:** ").append(n.get("routing_note").asText()).append("\n");
        }
        JsonNode entities = n.get("entities");
        if (entities != null) {
            sb.append("\n### Entities\n\n");
            if (entities.has("gene_symbols") && entities.get("gene_symbols").size() > 0) {
                sb.append("- **Gene symbols:** ");
                for (JsonNode g : entities.get("gene_symbols")) {
                    sb.append("`").append(g.asText()).append("` ");
                }
                sb.append("\n");
            }
            if (entities.has("disease_query")) {
                sb.append("- **Disease query:** ").append(entities.get("disease_query").asText()).append("\n");
            }
        }
        sb.append("\n### Tool sequence\n\n");
        for (JsonNode step : n.get("tool_sequence")) {
            sb.append("1. `").append(step.asText()).append("`\n");
        }
        sb.append("\n### Stop rules\n\n");
        for (JsonNode rule : n.get("stop_rules")) {
            sb.append("- ").append(rule.asText()).append("\n");
        }
        return sb.toString();
    }

    private static String formatIdentifierResolution(JsonNode n) {
        StringBuilder sb = new StringBuilder("## Identifier resolution\n\n");
        sb.append("- **Entity:** ").append(cell(n, "entity_type")).append("\n");
        sb.append("- **Canonical ID:** `").append(cell(n, "canonical_id")).append("`\n");
        sb.append("- **ID system:** ").append(cell(n, "id_system")).append("\n");
        if (n.has("symbol")) {
            sb.append("- **Symbol:** ").append(cell(n, "symbol")).append("\n");
        }
        if (n.has("name")) {
            sb.append("- **Name:** ").append(cell(n, "name")).append("\n");
        }
        if (n.has("ambiguous") && n.get("ambiguous").asBoolean()) {
            sb.append("\n**Ambiguous** — confirm with the user before proceeding.\n\n");
            if (n.has("candidates")) {
                sb.append("| Label | ID |\n|-------|----|\n");
                for (JsonNode c : n.get("candidates")) {
                    sb.append("| ")
                            .append(cell(c, "label"))
                            .append(" | `")
                            .append(cell(c, "id"))
                            .append("` |\n");
                }
            }
        }
        if (n.has("resolution_note")) {
            sb.append("\n_").append(n.get("resolution_note").asText()).append("_\n");
        }
        return sb.toString();
    }

    private static String formatGeneEvidencePayload(JsonNode wrapper) {
        JsonNode payload = wrapper.get("payload");
        String head =
                "## Target evidence (API)\n\n- **Gene:** `"
                        + cell(payload, "gene_id")
                        + "` ("
                        + cell(payload, "symbol")
                        + ")\n\n";
        return head + formatGeneEvidenceResponse(payload);
    }

    private static String formatGeneEvidenceResponse(JsonNode n) {
        StringBuilder sb = new StringBuilder("## Target evidence");
        if (n.has("symbol")) {
            sb.append(": **").append(n.get("symbol").asText()).append("**");
        }
        sb.append("\n\n| Disease | Score | Types |\n|---------|------:|-------|\n");
        if (!n.has("evidence") || !n.get("evidence").isArray()) {
            return sb + "\n_No evidence bundles._\n";
        }
        for (JsonNode bundle : n.get("evidence")) {
            String types = summarizeEvidenceTypes(bundle.get("evidence"));
            sb.append("| ")
                    .append(cell(bundle, "disease_name"))
                    .append(" | ")
                    .append(String.format("%.3f", bundle.get("score").asDouble()))
                    .append(" | ")
                    .append(types)
                    .append(" |\n");
        }
        return sb.toString();
    }

    private static String summarizeEvidenceTypes(JsonNode evidenceArr) {
        if (evidenceArr == null || !evidenceArr.isArray() || evidenceArr.isEmpty()) {
            return "—";
        }
        StringBuilder tb = new StringBuilder();
        for (JsonNode e : evidenceArr) {
            if (!tb.isEmpty()) {
                tb.append(", ");
            }
            tb.append(cell(e, "evidence_type"));
        }
        return tb.toString();
    }

    private static String formatExternalLinks(JsonNode n) {
        StringBuilder sb = new StringBuilder("## External links for **");
        sb.append(cell(n, "symbol")).append("**\n\n");
        if (!n.has("links")) {
            return sb + "_No links._\n";
        }
        for (JsonNode link : n.get("links")) {
            sb.append("- [")
                    .append(cell(link, "label"))
                    .append("](")
                    .append(cell(link, "url"))
                    .append(") (`")
                    .append(cell(link, "provider"))
                    .append("`)\n");
        }
        return sb.toString();
    }

    private static String formatProvenanceBundle(JsonNode n) {
        StringBuilder sb = new StringBuilder("## Provenance bundle\n\n");
        sb.append("- **Exported:** ").append(cell(n, "exported_at")).append("\n");
        sb.append("- **Gene:** ").append(cell(n, "symbol")).append(" (`").append(cell(n, "gene_id")).append("`)\n");
        sb.append("- **Verify UI:** ").append(cell(n, "verify_ui_url")).append("\n");
        if (n.has("meta") && n.get("meta").has("data_version")) {
            sb.append("- **Data version:** `").append(n.get("meta").get("data_version").asText()).append("`\n");
        }
        sb.append("\n### Queries run\n\n");
        for (JsonNode q : n.get("queries_run")) {
            sb.append("- `").append(q.asText()).append("`\n");
        }
        return sb.toString();
    }

    private static String formatTargetEvidence(JsonNode n) {
        StringBuilder sb = new StringBuilder("## Target evidence");
        if (n.has("symbol") && !n.get("symbol").asText().isBlank()) {
            sb.append(": **").append(n.get("symbol").asText()).append("**");
        }
        sb.append("\n\n");
        if (n.has("note")) {
            sb.append("_").append(n.get("note").asText()).append("_\n\n");
        }
        sb.append("| Disease | Score | Evidence types |\n|---------|------:|----------------|\n");
        for (JsonNode a : n.get("associations")) {
            String types = "association_score";
            if (a.has("evidence") && a.get("evidence").isArray() && a.get("evidence").size() > 0) {
                StringBuilder tb = new StringBuilder();
                for (JsonNode e : a.get("evidence")) {
                    if (!tb.isEmpty()) {
                        tb.append(", ");
                    }
                    tb.append(cell(e, "type"));
                }
                types = tb.toString();
            }
            sb.append("| ")
                    .append(cell(a, "disease_name"))
                    .append(" | ")
                    .append(String.format("%.3f", a.get("score").asDouble()))
                    .append(" | ")
                    .append(types)
                    .append(" |\n");
        }
        return sb.toString();
    }

    private static String findNodeName(JsonNode nodes, String id) {
        for (JsonNode node : nodes) {
            if (id.equals(node.get("id").asText())) {
                return node.has("name") && !node.get("name").isNull()
                        ? node.get("name").asText()
                        : id;
            }
        }
        return id;
    }

    private static String cell(JsonNode n, String field) {
        return n.has(field) && !n.get(field).isNull() ? n.get(field).asText() : "—";
    }

    private static Iterable<String> iterableText(JsonNode arr) {
        return () ->
                new java.util.Iterator<>() {
                    int i = 0;

                    @Override
                    public boolean hasNext() {
                        return i < arr.size();
                    }

                    @Override
                    public String next() {
                        return arr.get(i++).asText();
                    }
                };
    }
}
