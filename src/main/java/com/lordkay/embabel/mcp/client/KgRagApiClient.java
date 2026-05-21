package com.lordkay.embabel.mcp.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lordkay.embabel.mcp.config.KgRagProperties;

@Service
@ConditionalOnProperty(prefix = "kg-rag", name = "enabled", havingValue = "true")
public class KgRagApiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final String baseUrl;

    public KgRagApiClient(KgRagProperties properties) {
        this.baseUrl = properties.getApiBaseUrl().replaceAll("/$", "");
        this.restClient = RestClient.builder().baseUrl(this.baseUrl).build();
    }

    public String health() {
        return get("/health");
    }

    public String ask(String question) {
        try {
            String body = MAPPER.writeValueAsString(java.util.Map.of("question", question));
            return restClient
                    .post()
                    .uri(baseUrl + "/ask")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (JsonProcessingException e) {
            return "{\"error\":true,\"detail\":\"" + e.getMessage() + "\"}";
        } catch (Exception e) {
            return "{\"error\":true,\"detail\":\"KG RAG unreachable at " + baseUrl + ": " + e.getMessage() + "\"}";
        }
    }

    private String get(String path) {
        try {
            return restClient.get().uri(baseUrl + path).retrieve().body(String.class);
        } catch (Exception e) {
            return "{\"error\":true,\"detail\":\"" + e.getMessage() + "\"}";
        }
    }
}
