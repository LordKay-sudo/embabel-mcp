package com.lordkay.embabel.mcp.prompts;



import java.util.List;



import org.springaicommunity.mcp.annotation.McpArg;

import org.springaicommunity.mcp.annotation.McpPrompt;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import org.springframework.stereotype.Component;



import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;

import io.modelcontextprotocol.spec.McpSchema.PromptMessage;

import io.modelcontextprotocol.spec.McpSchema.Role;

import io.modelcontextprotocol.spec.McpSchema.TextContent;



/**

 * MCP prompt templates for common biomedical graph workflows (M2 intent routing).

 */

@Component

@ConditionalOnExpression("'${bioinsight.mcp.tool-profile:standard}' != 'minimal'")

public class BioInsightMcpPrompts {



    @McpPrompt(

            name = "summarize-gene-targets",

            description = "Investigate a gene symbol and summarize disease associations with scores")

    public GetPromptResult summarizeGeneTargets(

            @McpArg(name = "symbol", description = "Gene symbol e.g. BRCA1", required = true) String symbol) {

        String text =

                """

                BioInsight Graph MCP — **gene-first** workflow (do not use disease-first tools).



                1. `plan_investigation` with question="Summarize disease associations for %s" (format=json).

                2. `resolve_identifier` with query="%s", entityType=gene.

                3. If ambiguous, stop and ask the user which match to use.

                4. `build_target_dossier` with symbol="%s" (format=markdown).

                5. Summarize top associations and scores; cite data_version from the provenance footer.

                6. State: demo data only, not clinical advice.

                """

                        .formatted(symbol, symbol, symbol);

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

                BioInsight Graph MCP — **compare** workflow (not separate dossiers).



                1. `plan_investigation` with question="Compare %s and %s" intent=COMPARE_GENES.

                2. `resolve_identifier` for each symbol (entityType=gene); stop if either is ambiguous.

                3. `compare_genes` with symbols="%s,%s" (format=markdown).

                4. Explain overlapping vs unique top diseases.

                5. Demo sample only — not clinical advice.

                """

                        .formatted(symbolA, symbolB, symbolA, symbolB);

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

                BioInsight Graph MCP — **disease-first** workflow (do not call build_target_dossier first).



                1. `plan_investigation` with question="Top targets for %s" intent=DISEASE_TARGETS.

                2. `resolve_identifier` with query="%s", entityType=disease.

                3. If ambiguous, stop and ask which disease id to use.

                4. `get_disease_genes` with the canonical disease id, minScore=%s (format=markdown).

                5. Ranked table + brief interpretation of top 3 targets.

                """

                        .formatted(diseaseQuery, diseaseQuery, min);

        return prompt("Top targets for " + diseaseQuery, text);

    }



    @McpPrompt(

            name = "adaptive-gene-investigation",

            description =

                    "M6: Plan then execute gene investigation; branch on ambiguous symbols or missing ENSG")

    public GetPromptResult adaptiveGeneInvestigation(

            @McpArg(name = "question", description = "Natural language question about a gene/target", required = true)

                    String question) {

        String text =

                """

                Adaptive gene investigation (plan → resolve → execute):



                1. `plan_investigation` with question="%s" (format=json). Read intent and tool_sequence.

                2. `resolve_identifier` for each gene symbol in the plan (entityType=gene).

                3. **Branching:**

                   - If `ambiguous: true`, list candidates and ask the user — do **not** call build_target_dossier yet.

                   - If no gene found, suggest `search_genes` with a broader query.

                   - If intent is LITERATURE, run `build_target_dossier` first; call `kg_rag_ask` only if associations are sparse or the question asks about literature/mechanism.

                   - If intent is GENE_TARGET and resolution is clear, `build_target_dossier` then optional `get_target_evidence` for the top disease.

                4. Follow stop_rules from the plan. Cite provenance footer / bioinsight://meta.

                5. Demo data only.



                Resource: `bioinsight://investigation-playbook`

                """

                        .formatted(question);

        return prompt("Adaptive investigation", text);

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



                1. Ask the agent to run `plan_investigation` then `build_target_dossier` or `research_gene` for **%s** (markdown).

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

                    "Dual-channel: graph dossier first; KG RAG only when sparse or literature-focused")

    public GetPromptResult graphAndLiterature(

            @McpArg(name = "geneOrTopic", description = "Gene symbol or topic, e.g. BRCA1", required = true)

                    String geneOrTopic) {

        String text =

                """

                Dual-channel investigation (channel A = graph, channel B = literature):



                1. `plan_investigation` with question="%s literature and disease associations" intent=LITERATURE.

                2. `resolve_identifier` with query="%s", entityType=gene.

                3. **Channel A:** `build_target_dossier` for **%s** (format=markdown).

                4. **Channel B (conditional):** call `kg_rag_ask` **only if**:

                   - fewer than 3 diseases above min_score in the dossier, **or**

                   - the user explicitly asked about mechanism/literature/papers.

                5. Synthesize graph scores vs cited document chunks; label each channel.

                6. Demo samples only — not clinical advice.

                """

                        .formatted(geneOrTopic, geneOrTopic, geneOrTopic);

        return prompt("Graph + literature: " + geneOrTopic, text);

    }



    @McpPrompt(

            name = "public-data-to-mcp-tutorial",

            description =

                    "M13: New-contributor path — public data → local BioInsight API → this MCP server (no Java edits)")

    public GetPromptResult publicDataToMcpTutorial(

            @McpArg(name = "geneSymbol", description = "Gene to verify end-to-end, e.g. BRCA1", required = false)

                    String geneSymbol) {

        String symbol = geneSymbol != null && !geneSymbol.isBlank() ? geneSymbol : "BRCA1";

        String text =

                """

                End-to-end setup: public Open Targets data → local BioInsight API → MCP in Cursor.

                No Java code changes required — this is the contributor onboarding path.



                **1. Bring up BioInsight Graph (the data + API).**

                   - Clone `bioinsight-graph`. From its root run `docker compose up --build`.

                   - This seeds the frozen Open Targets slice (CC0 public data) into Neo4j and serves the API.

                   - Verify: open http://localhost:8000/docs and http://localhost:8080 (UI). See its `docs/PLATFORM.md`.



                **2. Point this MCP server at the API.**

                   - Set `BIOINSIGHT_API_BASE_URL=http://localhost:8000/api/v1` (default already matches).

                   - Optionally set `BIOINSIGHT_WEB_UI_URL=http://localhost:8080` for provenance links.

                   - Pick a tool surface: `BIOINSIGHT_MCP_TOOL_PROFILE=minimal|standard|full`.

                   - Run the MCP server (`./mvnw spring-boot:run`) or register it in your Cursor MCP config.



                **3. Verify the chain in Cursor.**

                   - Call `check_bioinsight_health` — confirm the API is reachable.

                   - Call `plan_investigation` with question="Summarize disease associations for %s".

                   - Call `resolve_identifier` query="%s" entityType=gene, then `build_target_dossier` symbol="%s".

                   - Confirm the dossier's provenance footer shows `data_version` from `/meta`.



                **4. Cross-check (human-in-the-loop).**

                   - Open the BioInsight UI, search **%s**, and compare scores/IDs to the dossier.

                   - Use the `review-gene-report` prompt for the full review checklist.



                Notes: demo Open Targets–style data only — associations are not causation and this is not clinical advice.

                Resources: `bioinsight://meta`, `bioinsight://investigation-playbook`.

                """

                        .formatted(symbol, symbol, symbol, symbol);

        return prompt("Tutorial: public data -> API -> MCP", text);

    }



    private static GetPromptResult prompt(String title, String userText) {

        return new GetPromptResult(

                title, List.of(new PromptMessage(Role.USER, new TextContent(userText))));

    }

}


