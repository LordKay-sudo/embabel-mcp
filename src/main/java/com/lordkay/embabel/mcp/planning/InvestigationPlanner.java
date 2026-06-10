package com.lordkay.embabel.mcp.planning;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Builds JSON investigation plans (M1) and classifies intent for prompt routing (M2).
 * Heuristic only — no BioInsight API calls; execution tools run after the plan.
 */
public final class InvestigationPlanner {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern GENE_SYMBOL = Pattern.compile("\\b([A-Z][A-Z0-9]{1,9})\\b");
    private static final Pattern QUOTED = Pattern.compile("[\"']([^\"']+)[\"']");

    private InvestigationPlanner() {}

    public static InvestigationIntent routeIntent(String question) {
        if (question == null || question.isBlank()) {
            return InvestigationIntent.GENERAL;
        }
        String q = question.toLowerCase(Locale.ROOT);

        if (containsAny(q, "literature", "pubmed", "paper", "mechanism", "pathway", "study", "article")) {
            return InvestigationIntent.LITERATURE;
        }
        if (containsAny(q, "compare", " versus ", " vs ", " vs.", "difference between", "overlap")) {
            return InvestigationIntent.COMPARE_GENES;
        }
        if (containsAny(
                q,
                "disease",
                "cancer",
                "targets for",
                "genes for",
                "top targets",
                "which genes",
                "what genes")) {
            return InvestigationIntent.DISEASE_TARGETS;
        }
        if (containsAny(q, "gene", "symbol", "target dossier", "associations for", "diseases for")) {
            return InvestigationIntent.GENE_TARGET;
        }
        if (extractGeneSymbols(question).size() >= 2) {
            return InvestigationIntent.COMPARE_GENES;
        }
        if (extractGeneSymbols(question).size() == 1) {
            return InvestigationIntent.GENE_TARGET;
        }
        return InvestigationIntent.GENERAL;
    }

    public static String buildPlanJson(String question, InvestigationIntent intentOverride) {
        String q = question == null ? "" : question.trim();
        InvestigationIntent intent = intentOverride != null ? intentOverride : routeIntent(q);

        ObjectNode plan = MAPPER.createObjectNode();
        plan.put("question", q);
        plan.put("intent", intent.name());

        ObjectNode entities = plan.putObject("entities");
        List<String> genes = extractGeneSymbols(q);
        String diseaseHint = extractDiseaseHint(q, genes);
        ArrayNode geneArr = entities.putArray("gene_symbols");
        genes.forEach(geneArr::add);
        if (diseaseHint != null) {
            entities.put("disease_query", diseaseHint);
        }

        ArrayNode tools = plan.putArray("tool_sequence");
        for (String step : toolSequence(intent)) {
            tools.add(step);
        }

        ArrayNode stops = plan.putArray("stop_rules");
        for (String rule : stopRules(intent)) {
            stops.add(rule);
        }

        plan.put("routing_note", routingNote(intent));
        return plan.toString();
    }

    private static List<String> toolSequence(InvestigationIntent intent) {
        return switch (intent) {
            case GENE_TARGET -> List.of(
                    "resolve_identifier(entity_type=gene)",
                    "build_target_dossier",
                    "get_target_evidence (optional, per top disease)");
            case DISEASE_TARGETS -> List.of(
                    "resolve_identifier(entity_type=disease)",
                    "get_disease_genes",
                    "get_disease (if metadata needed)");
            case COMPARE_GENES -> List.of(
                    "resolve_identifier for each symbol",
                    "compare_genes",
                    "get_target_evidence (optional, overlapping diseases only)");
            case LITERATURE -> List.of(
                    "resolve_identifier(entity_type=gene)",
                    "build_target_dossier",
                    "kg_rag_ask (only if sparse graph evidence or explicit literature question)");
            case GENERAL -> List.of(
                    "plan_investigation (refine intent)",
                    "resolve_identifier",
                    "intent-specific primary tool (see bioinsight://investigation-playbook)");
        };
    }

    private static List<String> stopRules(InvestigationIntent intent) {
        List<String> rules = new ArrayList<>();
        rules.add("If resolve_identifier returns multiple candidates, stop and ask the user to pick one.");
        rules.add("If primary tool returns error or empty associations, stop — do not invent clinical claims.");
        rules.add("Always cite data_version from dossier footer or bioinsight://meta.");
        if (intent == InvestigationIntent.LITERATURE) {
            rules.add("Call kg_rag_ask only when graph associations are sparse (<3 above min_score) or user asked about literature/mechanism.");
        }
        if (intent == InvestigationIntent.COMPARE_GENES) {
            rules.add("Require at least two resolved gene symbols before compare_genes.");
        }
        return rules;
    }

    private static String routingNote(InvestigationIntent intent) {
        return switch (intent) {
            case GENE_TARGET -> "Gene-first: prefer build_target_dossier over many get_* calls.";
            case DISEASE_TARGETS -> "Disease-first: search/resolve disease id, then get_disease_genes — not build_target_dossier.";
            case COMPARE_GENES -> "Compare path: compare_genes after resolving symbols — not separate dossiers unless user asks.";
            case LITERATURE -> "Dual-channel: graph dossier first (channel A); kg_rag_ask only when needed (channel B).";
            case GENERAL -> "Intent unclear — resolve entities then re-run plan_investigation or read investigation-playbook.";
        };
    }

    static List<String> extractGeneSymbols(String text) {
        Set<String> symbols = new LinkedHashSet<>();
        Matcher m = GENE_SYMBOL.matcher(text);
        while (m.find()) {
            String sym = m.group(1);
            if (!isStopword(sym)) {
                symbols.add(sym);
            }
        }
        return List.copyOf(symbols);
    }

    private static String extractDiseaseHint(String text, List<String> geneSymbols) {
        Matcher q = QUOTED.matcher(text);
        if (q.find()) {
            return q.group(1).trim();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (String prefix : List.of("targets for ", "genes for ", "disease ", "for disease ")) {
            int idx = lower.indexOf(prefix);
            if (idx >= 0) {
                String tail = text.substring(idx + prefix.length()).trim();
                tail = tail.replaceAll("[?.!]$", "").trim();
                if (!tail.isBlank() && !geneSymbols.contains(tail.toUpperCase(Locale.ROOT))) {
                    return tail;
                }
            }
        }
        return null;
    }

    private static boolean isStopword(String sym) {
        return Set.of("GET", "API", "MCP", "DNA", "RNA", "HITL", "JSON", "HTTP", "ENSG", "EFO", "MONDO")
                .contains(sym);
    }

    private static boolean containsAny(String haystack, String... needles) {
        for (String n : needles) {
            if (haystack.contains(n)) {
                return true;
            }
        }
        return false;
    }
}
