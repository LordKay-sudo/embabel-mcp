package com.lordkay.embabel.mcp.tools;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lordkay.embabel.mcp.client.OntoHarnessApiClient;
import com.lordkay.embabel.mcp.config.OntoHarnessProperties;
import com.lordkay.embabel.mcp.format.BioInsightMarkdown;

/**
 * OntoHarness MCP tools — deterministic SHACL + vocabulary validation for proposed RDF.
 */
@Component
public class OntoHarnessMcpTools {

    private final OntoHarnessApiClient client;
    private final OntoHarnessProperties properties;
    private final ObjectMapper objectMapper;

    public OntoHarnessMcpTools(
            OntoHarnessApiClient client, OntoHarnessProperties properties, ObjectMapper objectMapper) {
        this.client = client;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @McpTool(
            name = "validate_proposal",
            description =
                    "Validate RDF Turtle against an OntoHarness domain (vocab gate + SHACL). "
                            + "Use before writing graph mutations from LLM-extracted triples.")
    public String validateProposal(
            @McpToolParam(description = "RDF Turtle payload to validate", required = true) String turtle,
            @McpToolParam(description = "Domain name (default biomedical)", required = false) String domain,
            @McpToolParam(description = "markdown or json (default json)", required = false) String format) {
        if (!properties.isEnabled()) {
            return disabledMessage(format);
        }
        String json = client.validate(domain == null ? properties.getDefaultDomain() : domain, turtle);
        return formatResponse(json, format);
    }

    @McpTool(
            name = "get_repair_hints",
            description =
                    "Validate Turtle and return only repair_hints for agent correction loops.")
    public String getRepairHints(
            @McpToolParam(description = "RDF Turtle payload", required = true) String turtle,
            @McpToolParam(description = "Domain name", required = false) String domain) {
        if (!properties.isEnabled()) {
            return disabledMessage("json");
        }
        String json = client.validate(domain == null ? properties.getDefaultDomain() : domain, turtle);
        try {
            JsonNode node = objectMapper.readTree(json);
            return objectMapper.writeValueAsString(
                    java.util.Map.of(
                            "conforms", node.path("conforms").asBoolean(false),
                            "repair_hints", node.path("repair_hints")));
        } catch (Exception ex) {
            return "{\"conforms\":false,\"repair_hints\":[\"Failed to parse validation response\"]}";
        }
    }

    @McpTool(
            name = "list_ontoharness_domains",
            description = "List registered OntoHarness validation domains.")
    public String listDomains(
            @McpToolParam(description = "markdown or json", required = false) String format) {
        if (!properties.isEnabled()) {
            return disabledMessage(format);
        }
        return formatResponse(client.listDomains(), format);
    }

    @McpTool(
            name = "bridge_gap_record",
            description =
                    "Project a GapForge-shaped gap record (JSON) to Turtle via OntoHarness v0.5 bridge, "
                            + "optionally running vocab gate + SHACL. Use before propose when testing RDF shape.")
    public String bridgeGapRecord(
            @McpToolParam(
                            description =
                                    "JSON object: id, claim, confidence, gap_class?, genes?, disease?, approved_at?, provenance_hash?",
                            required = true)
                    String recordJson,
            @McpToolParam(description = "Domain name", required = false) String domain,
            @McpToolParam(description = "Run validation after projection (default true)", required = false)
                    Boolean runValidation,
            @McpToolParam(description = "markdown or json (default json)", required = false) String format) {
        if (!properties.isEnabled()) {
            return disabledMessage(format);
        }
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> record =
                    objectMapper.readValue(recordJson, java.util.Map.class);
            boolean validate = runValidation == null || runValidation;
            String dom = domain == null ? properties.getDefaultDomain() : domain;
            String json = client.bridgeGapRecord(dom, record, validate);
            if ("markdown".equalsIgnoreCase(format)) {
                JsonNode node = objectMapper.readTree(json);
                String turtle = node.path("turtle").asText("");
                return "```turtle\n" + turtle + "\n```\n\n" + BioInsightMarkdown.format(json);
            }
            return json;
        } catch (Exception ex) {
            return "{\"conforms\":false,\"error\":\"bridge_failed\",\"detail\":\""
                    + ex.getMessage().replace("\"", "'")
                    + "\"}";
        }
    }

    private String disabledMessage(String format) {
        String msg =
                "{\"enabled\":false,\"message\":\"Set ONTOHARNESS_ENABLED=true and start the OntoHarness sidecar (port 8010).\"}";
        if ("markdown".equalsIgnoreCase(format)) {
            return "OntoHarness is **disabled**. Enable `ONTOHARNESS_ENABLED` and run the sidecar.";
        }
        return msg;
    }

    private String formatResponse(String json, String format) {
        if ("markdown".equalsIgnoreCase(format)) {
            return BioInsightMarkdown.format(json);
        }
        return json;
    }
}
