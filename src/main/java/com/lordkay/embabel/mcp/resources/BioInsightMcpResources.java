package com.lordkay.embabel.mcp.resources;

import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import com.lordkay.embabel.mcp.client.BioInsightApiClient;
import com.lordkay.embabel.mcp.format.BioInsightMarkdown;

/**
 * Read-only MCP resources for graph schema, provenance, and live statistics.
 */
@Component
public class BioInsightMcpResources {

    private static final String SCHEMA = """
            # BioInsight Graph schema

            ## Nodes
            - `(:Gene {id, symbol, name})`
            - `(:Disease {id, name})`
            - `(:Protein {id, name})`

            ## Relationships
            - `(:Gene)-[:ASSOCIATED_WITH {score, source}]->(:Disease)`
            - `(:Protein)-[:ENCODED_BY]->(:Gene)`

            ## Example Cypher
            ```cypher
            MATCH (g:Gene {symbol: 'BRCA1'})-[r:ASSOCIATED_WITH]->(d:Disease)
            RETURN g.symbol, d.name, r.score
            ORDER BY r.score DESC
            LIMIT 10
            ```
            """;

    private static final String ECOSYSTEM = """
            # LordKay research platform (local URLs)

            | Application | Browser | API | Role |
            |-------------|---------|-----|------|
            | **BioInsight Graph** | http://localhost:8080 | http://localhost:8000/docs | Structured disease–target graph |
            | **Neo4j (BioInsight)** | http://localhost:7474 | bolt://localhost:7687 | Graph storage |
            | **KG RAG Demo** | http://localhost:5173 (dev) | http://localhost:8001/docs | Document RAG + citations |
            | **Neo4j (KG RAG)** | http://localhost:7475 | bolt://localhost:7688 | Separate graph (no port clash) |
            | **embabel-mcp** | — | http://localhost:1337/sse | MCP tools + agents for Cursor |

            Enable KG RAG bridge: `KG_RAG_ENABLED=true` and run kg-rag-demo API on port 8001.
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
}
