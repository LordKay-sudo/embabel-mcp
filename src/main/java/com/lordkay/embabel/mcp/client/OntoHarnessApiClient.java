package com.lordkay.embabel.mcp.client;

import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lordkay.embabel.mcp.config.OntoHarnessProperties;

@Service
public class OntoHarnessApiClient {

    private final RestClient restClient;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public OntoHarnessApiClient(OntoHarnessProperties properties, ObjectMapper objectMapper) {
        this.baseUrl = properties.getApiBaseUrl().replaceAll("/$", "");
        this.restClient = RestClient.builder().baseUrl(this.baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public String validate(String domain, String turtle) {
        try {
            String body =
                    objectMapper.writeValueAsString(
                            Map.of("domain", domain, "format", "turtle", "content", turtle));
            URI uri =
                    UriComponentsBuilder.fromHttpUrl(baseUrl)
                            .path("/api/v1/validate")
                            .build(true)
                            .toUri();
            return restClient
                    .post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        String err = new String(response.getBody().readAllBytes());
                        throw new BioInsightApiException(response.getStatusCode().value(), err);
                    })
                    .body(String.class);
        } catch (JsonProcessingException ex) {
            return errorJson(500, "Failed to encode validation request: " + ex.getMessage());
        } catch (BioInsightApiException ex) {
            return errorJson(ex.status(), ex.body());
        } catch (Exception ex) {
            return errorJson(503, "Failed to reach OntoHarness at " + baseUrl + ": " + ex.getMessage());
        }
    }

    public String listDomains() {
        URI uri =
                UriComponentsBuilder.fromHttpUrl(baseUrl).path("/api/v1/domains").build(true).toUri();
        try {
            return restClient.get().uri(uri).retrieve().body(String.class);
        } catch (Exception ex) {
            return errorJson(503, "Failed to reach OntoHarness at " + baseUrl + ": " + ex.getMessage());
        }
    }

    private static String errorJson(int status, String message) {
        String safe = message == null ? "" : message.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"conforms\":false,\"error\":" + status + ",\"detail\":\"" + safe + "\"}";
    }
}
