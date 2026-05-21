package com.lordkay.embabel.mcp.prompts;

import java.util.List;

import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springframework.stereotype.Component;

import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

/**
 * MCP prompt templates for common biomedical graph workflows.
 */
@Component
public class BioInsightMcpPrompts {

    @McpPrompt(
            name = "summarize-gene-targets",
            description = "Investigate a gene symbol and summarize disease associations with scores")
    public GetPromptResult summarizeGeneTargets(
            @McpArg(name = "symbol", description = "Gene symbol e.g. BRCA1", required = true) String symbol) {
        String text =
                """
                You have access to BioInsight Graph MCP tools (demo Open Targets–style data).

                1. Call `investigate_gene_symbol` with symbol="%s" (format=markdown).
                2. Summarize the strongest disease associations and note association scores.
                3. State clearly that this is demo data, not clinical advice.
                """
                        .formatted(symbol);
        return prompt("Summarize targets for " + symbol, text);
    }

    @McpPrompt(
            name = "compare-gene-pair",
            description = "Compare two genes and highlight shared vs distinct disease links")
    public GetPromptResult compareGenePair(
            @McpArg(name = "symbolA", required = true) String symbolA,
            @McpArg(name = "symbolB", required = true) String symbolB) {
        String text =
                """
                Use BioInsight Graph MCP tools:

                1. `compare_genes` with symbols="%s,%s" (format=markdown).
                2. Explain overlapping diseases vs unique top associations per gene.
                3. Mention data is a research demo sample only.
                """
                        .formatted(symbolA, symbolB);
        return prompt("Compare " + symbolA + " vs " + symbolB, text);
    }

    @McpPrompt(
            name = "top-targets-for-disease",
            description = "List top gene targets for a disease by association score")
    public GetPromptResult topTargetsForDisease(
            @McpArg(name = "diseaseQuery", description = "Disease name or id fragment", required = true)
                    String diseaseQuery,
            @McpArg(name = "minScore", description = "Minimum score 0-1", required = false) String minScore) {
        String min = minScore != null ? minScore : "0.3";
        String text =
                """
                Use BioInsight Graph MCP tools:

                1. `search_diseases` with query="%s".
                2. Pick the best matching disease id, then `get_disease_genes` with minScore=%s (format=markdown).
                3. Present a ranked table of targets and interpret the top 3 briefly.
                """
                        .formatted(diseaseQuery, min);
        return prompt("Top targets for " + diseaseQuery, text);
    }

    @McpPrompt(
            name = "review-gene-report",
            description =
                    "Human-in-the-loop: verify BioInsight UI before accepting an agent gene report")
    public GetPromptResult reviewGeneReport(
            @McpArg(name = "symbol", description = "Gene symbol to verify, e.g. BRCA1", required = true)
                    String symbol) {
        String text =
                """
                Human-in-the-loop workflow (you are the reviewer):

                1. Ask the agent to run `investigate_gene_symbol` or `research_gene` for **%s** (markdown).
                2. Open http://localhost:8080 — search **%s** and open the gene detail page.
                3. Compare: Ensembl ID, top disease scores, and graph neighbors vs the agent report.
                4. If they match, summarize for the user; if not, list discrepancies and do not claim clinical validity.
                5. State: demo Open Targets–style sample only.

                Optional: read MCP resource `bioinsight://human-in-the-loop`.
                """
                        .formatted(symbol, symbol);
        return prompt("HITL review: " + symbol, text);
    }

    @McpPrompt(
            name = "graph-and-literature",
            description =
                    "Combine BioInsight graph evidence with KG RAG document Q&A (requires KG_RAG_ENABLED)")
    public GetPromptResult graphAndLiterature(
            @McpArg(name = "geneOrTopic", description = "Gene symbol or topic, e.g. BRCA1", required = true)
                    String geneOrTopic) {
        String text =
                """
                Use BioInsight Graph MCP tools and (if available) KG RAG tools:

                1. `research_gene` or `investigate_gene_symbol` for **%s** (markdown).
                2. If `kg_rag_ask` is available, ask: "What does the literature say about %s and disease associations?"
                3. Synthesize: structured scores from the graph vs cited sentences from documents.
                4. State both are demo samples, not clinical advice.
                """
                        .formatted(geneOrTopic, geneOrTopic);
        return prompt("Graph + literature: " + geneOrTopic, text);
    }

    private static GetPromptResult prompt(String title, String userText) {
        return new GetPromptResult(
                title, List.of(new PromptMessage(Role.USER, new TextContent(userText))));
    }
}
