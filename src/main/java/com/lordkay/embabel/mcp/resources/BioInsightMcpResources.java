package com.lordkay.embabel.mcp.resources;

import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.lordkay.embabel.mcp.client.BioInsightApiClient;
import com.lordkay.embabel.mcp.format.BioInsightMarkdown;

/**
 * Read-only MCP resources for graph schema, provenance, and live statistics.
 */
@Component
public class BioInsightMcpResources {

    private static final String SCHEMA =
            """
            # BioInsight schema (summary)

            Nodes: Gene, Disease, Protein. Edge: ASSOCIATED_WITH {score}, ENCODED_BY.
            Prefer MCP tool `build_target_dossier` over loading full subgraph JSON.
            Full model: bioinsight-graph repo / docs/ARCHITECTURE.md
            """;

    private static final String PROVENANCE = """
            # Data provenance

            Live metadata: use MCP resource `bioinsight://meta` or BioInsight `GET /api/v1/meta`.

            Full document: https://github.com/LordKay-sudo/bioinsight-graph/blob/main/PROVENANCE.md

            - Demo/sample Open Targets–style associations — **not for clinical use**
            - Associations are **correlative**, not causal
            """;

    private final BioInsightApiClient api;

    public BioInsightMcpResources(BioInsightApiClient api) {
        this.api = api;
    }

    @McpResource(
            uri = "bioinsight://schema",
            name = "Graph schema",
            description = "Neo4j node/relationship model and example Cypher queries")
    public String schema() {
        return SCHEMA;
    }

    @McpResource(
            uri = "bioinsight://provenance",
            name = "Data provenance",
            description = "Dataset limitations and links to PROVENANCE.md")
    public String provenance() {
        return PROVENANCE;
    }

    @McpResource(
            uri = "bioinsight://meta",
            name = "Live dataset metadata",
            description = "Data version, release date, sources, and disclaimer from GET /api/v1/meta")
    public String liveMeta() {
        String json = api.get("/meta");
        if (json.contains("\"error\":true")) {
            return PROVENANCE + "\n\n_Meta API unreachable._\n\n```json\n" + json + "\n```\n";
        }
        return BioInsightMarkdown.format(json) + "\n";
    }

    @McpResource(
            uri = "bioinsight://stats",
            name = "Live graph statistics",
            description = "Current node and relationship counts from the running API")
    public String liveStats() {
        String json = api.get("/stats");
        return "# Live graph statistics\n\n```json\n" + json + "\n```\n";
    }

    @McpResource(
            uri = "bioinsight://context-policy",
            name = "Response and context policy",
            description =
                    "Full-fidelity tool responses; compact modes; tool profiles — see docs/RESPONSE_POLICY.md")
    public String contextPolicy() {
        return """
                # MCP response policy (summary)

                - Tool output is assembled fully, then `finish()` runs once (never mid-request).
                - Default: `compact-mode=off` — no truncation of biomedical evidence.
                - `build_target_dossier` / `investigate_gene_symbol` are never truncated.
                - `tool-profile`: minimal | standard | full — reduces tools at MCP connect.
                - 120k tokens is the host model budget; char limits here are not token limits.

                Docs: https://github.com/LordKay-sudo/embabel-mcp/blob/main/docs/RESPONSE_POLICY.md
                """;
    }
}

@Component
@ConditionalOnExpression("'${bioinsight.mcp.tool-profile:standard}' != 'minimal'")
class BioInsightMcpExtendedResources {

    private static final String ECOSYSTEM =
            """
            # Platform URLs (local)

            BioInsight UI :8080 · API :8000 · MCP :1337/sse · KG RAG optional :8001
            See bioinsight-graph docs/ARCHITECTURE.md
            """;

    @McpResource(
            uri = "bioinsight://human-in-the-loop",
            name = "Human-in-the-loop guide",
            description = "When and how humans should verify graph results (UI, MCP, Embabel WaitFor)")
    public String humanInTheLoop() {
        return """
                # Human-in-the-loop

                1. **BioInsight UI** — http://localhost:8080 (ground truth)
                2. **MCP prompt** — `review-gene-report`
                3. **Embabel agent** — `bioinsight.hitl.enabled=true` for WaitFor approval forms

                MCP mode defaults to auto-approve so Cursor is not blocked.

                Full doc: https://github.com/LordKay-sudo/bioinsight-graph/blob/main/docs/HUMAN_IN_THE_LOOP.md
                """;
    }

    @McpResource(
            uri = "bioinsight://ecosystem",
            name = "Research platform URLs",
            description = "Browser and API endpoints for BioInsight, KG RAG, Neo4j, and this MCP server")
    public String ecosystem() {
        return ECOSYSTEM;
    }

    @McpResource(
            uri = "bioinsight://investigation-playbook",
            name = "Investigation playbook",
            description = "Intent routing: which tool to use for gene, disease, compare, and literature questions")
    public String investigationPlaybook() {
        return """
                # Investigation playbook

                Always start with `plan_investigation` for non-trivial questions.

                | Intent | Primary tools | Avoid |
                |--------|---------------|-------|
                | Gene / target | resolve_identifier → build_target_dossier | get_disease_genes first |
                | Disease / targets | resolve_identifier(disease) → get_disease_genes | build_target_dossier |
                | Compare 2+ genes | resolve each → compare_genes | Multiple dossiers unless asked |
                | Literature | dossier → kg_rag_ask (if sparse or explicit) | kg_rag before graph |

                **Ambiguity:** if resolve_identifier returns candidates, stop and ask the user.

                **Evidence:** `get_target_evidence` after dossier for typed breakdown via `/genes/{id}/evidence`.
                **Resolve:** `resolve_identifier` calls BioInsight `GET /resolve` (ontology-aware; ambiguity in `candidates`).

                **Profiles:** minimal = plan + dossier + search; standard adds resolve + evidence; full adds neighbors/subgraph.

                Prompt: `adaptive-gene-investigation` for plan → resolve → branch execution.
                """;
    }
}
