package com.lordkay.embabel.mcp.resources;

import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import com.lordkay.embabel.mcp.client.BioInsightApiClient;

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

    private static final String PROVENANCE = """
            # Data provenance

            - **Source:** Representative sample inspired by [Open Targets](https://www.opentargets.org/)
            - **Scope:** ~30 genes, ~12 diseases, ~105 associations (demo MVP)
            - **Not for clinical use** — suitable for portfolio demos and agent tooling tests
            - **API:** https://github.com/LordKay-sudo/bioinsight-graph
            - **MCP:** https://github.com/LordKay-sudo/embabel-mcp
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
            description = "Dataset limitations and Open Targets–style sample notice")
    public String provenance() {
        return PROVENANCE;
    }

    @McpResource(
            uri = "bioinsight://stats",
            name = "Live graph statistics",
            description = "Current node and relationship counts from the running API")
    public String liveStats() {
        String json = api.get("/stats");
        return "# Live graph statistics\n\n```json\n" + json + "\n```\n";
    }
}
