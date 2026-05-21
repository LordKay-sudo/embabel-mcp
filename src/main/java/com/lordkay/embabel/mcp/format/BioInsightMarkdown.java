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
