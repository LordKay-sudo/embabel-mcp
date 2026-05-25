package com.lordkay.embabel.mcp.tools;

import com.lordkay.embabel.mcp.client.BioInsightApiClient;
import com.lordkay.embabel.mcp.config.BioInsightProperties;
import com.lordkay.embabel.mcp.config.McpContextProperties;
import com.lordkay.embabel.mcp.format.BioInsightMarkdown;
import com.lordkay.embabel.mcp.format.BioInsightProvenance;
import com.lordkay.embabel.mcp.format.McpResponseCompactor;
import com.lordkay.embabel.mcp.util.GeneIdParser;

/**
 * Shared formatting, provenance, and response-size limits for BioInsight MCP tools.
 */
abstract class BioInsightToolSupport {

    protected final BioInsightApiClient api;
    protected final BioInsightProperties properties;
    protected final McpContextProperties context;

    protected BioInsightToolSupport(
            BioInsightApiClient api, BioInsightProperties properties, McpContextProperties context) {
        this.api = api;
        this.properties = properties;
        this.context = context;
    }

    protected String respond(String json, String format) {
        return respond(json, format, false);
    }

    protected String respond(String json, String format, boolean defaultJson) {
        String out;
        if (wantsMarkdown(format) || (!defaultJson && !wantsJson(format))) {
            out = BioInsightMarkdown.format(json);
        } else {
            out = json;
        }
        return McpResponseCompactor.finish(out, context, McpResponseCompactor.ResponseKind.standard);
    }

    protected String markdownDossier(
            String symbol,
            String geneId,
            String detail,
            String diseases,
            String neighbors,
            String stats) {
        StringBuilder sb = new StringBuilder("# Target dossier: ").append(symbol).append("\n\n");
        sb.append(BioInsightMarkdown.format(detail)).append("\n");
        sb.append(BioInsightMarkdown.format(diseases)).append("\n");
        sb.append(BioInsightMarkdown.format(neighbors)).append("\n");
        if (stats != null) {
            sb.append(BioInsightMarkdown.format(stats)).append("\n");
        }
        String body = sb.append(BioInsightProvenance.footer(api, properties.getWebUiBaseUrl(), geneId)).toString();
        return McpResponseCompactor.finish(body, context, McpResponseCompactor.ResponseKind.workflow);
    }

    protected String resolveGeneId(String symbol) {
        String searchJson = api.get("/genes", java.util.Map.of("q", symbol));
        if (searchJson.contains("\"error\":true")) {
            return null;
        }
        return GeneIdParser.extractFirstGeneId(searchJson);
    }

    protected static boolean wantsMarkdown(String format) {
        return format != null && format.equalsIgnoreCase("markdown");
    }

    protected static boolean wantsJson(String format) {
        return format != null && format.equalsIgnoreCase("json");
    }
}
