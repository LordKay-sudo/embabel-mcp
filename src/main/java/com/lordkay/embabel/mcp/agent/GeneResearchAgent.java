package com.lordkay.embabel.mcp.agent;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Export;
import com.embabel.agent.domain.io.UserInput;
import com.lordkay.embabel.mcp.client.BioInsightApiClient;
import com.lordkay.embabel.mcp.domain.GeneGraphBundle;
import com.lordkay.embabel.mcp.domain.GeneResearchReport;
import com.lordkay.embabel.mcp.domain.GeneSymbolQuery;
import com.lordkay.embabel.mcp.format.BioInsightMarkdown;
import com.lordkay.embabel.mcp.util.GeneIdParser;

/**
 * Embabel agent published to MCP as {@code research_gene} — multi-step graph lookup
 * without mutating the BioInsight Graph application.
 */
@Agent(
        name = "GeneResearchAgent",
        description =
                "Research a gene symbol: resolve ID, ranked disease associations, and neighborhood summary",
        beanName = "geneResearchAgent")
public class GeneResearchAgent {

    private static final Pattern SYMBOL = Pattern.compile("\\b([A-Z][A-Z0-9]{1,14})\\b");
    private static final Pattern AFTER_GENE =
            Pattern.compile("(?i)\\bgene\\s+([A-Z][A-Z0-9]{1,14})\\b");

    private final BioInsightApiClient api;

    public GeneResearchAgent(BioInsightApiClient api) {
        this.api = api;
    }

    @Action
    public GeneSymbolQuery parseSymbol(UserInput input) {
        return new GeneSymbolQuery(extractSymbol(input.getContent()));
    }

    @Action
    public GeneGraphBundle loadGraphData(GeneSymbolQuery query) {
        String search = api.get("/genes", Map.of("q", query.symbol()));
        if (search.contains("\"error\":true")) {
            throw new IllegalStateException("BioInsight search failed: " + search);
        }
        String geneId = GeneIdParser.extractFirstGeneId(search);
        if (geneId == null) {
            throw new IllegalStateException("No gene found for symbol: " + query.symbol());
        }
        String detail = api.get("/genes/" + geneId);
        String diseases = api.get("/genes/" + geneId + "/diseases", Map.of("limit", "15"));
        String neighbors = api.get("/genes/" + geneId + "/neighbors");
        return new GeneGraphBundle(query.symbol(), geneId, detail, diseases, neighbors);
    }

    @AchievesGoal(
            description = "Produce a markdown research report for a gene symbol (demo data only)",
            export =
                    @Export(
                            remote = true,
                            name = "research_gene",
                            startingInputTypes = {GeneSymbolQuery.class, UserInput.class}))
    @Action
    public GeneResearchReport writeReport(GeneGraphBundle bundle) {
        StringBuilder md = new StringBuilder();
        md.append("# Gene research report: **").append(bundle.symbol()).append("**\n\n");
        md.append("_Demo Open Targets–style sample — not for clinical use._\n\n");
        md.append("**Ensembl ID:** `").append(bundle.geneId()).append("`\n\n");
        md.append(BioInsightMarkdown.format(bundle.detailJson())).append("\n");
        md.append(BioInsightMarkdown.format(bundle.diseasesJson())).append("\n");
        md.append(BioInsightMarkdown.format(bundle.neighborsJson())).append("\n");
        md.append("---\n\n");
        md.append("Explore in browser: http://localhost:8080/gene/").append(bundle.geneId()).append("\n");
        return new GeneResearchReport(md.toString());
    }

    static String extractSymbol(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Gene symbol is required");
        }
        String trimmed = raw.trim();
        String upper = trimmed.toUpperCase();
        Matcher genePhrase = AFTER_GENE.matcher(trimmed);
        if (genePhrase.find()) {
            return genePhrase.group(1).toUpperCase();
        }
        Matcher matcher = SYMBOL.matcher(upper);
        String last = null;
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (!isStopword(candidate)) {
                last = candidate;
            }
        }
        if (last != null) {
            return last;
        }
        String[] parts = trimmed.split("\\s+");
        return parts[parts.length - 1].toUpperCase();
    }

    private static boolean isStopword(String word) {
        return switch (word) {
            case "PLEASE", "RESEARCH", "GENE", "FOR", "THE", "AND", "WHAT", "SHOW", "FIND" -> true;
            default -> word.length() <= 2;
        };
    }
}
