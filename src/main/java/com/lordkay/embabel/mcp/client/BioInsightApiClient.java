package com.lordkay.embabel.mcp.client;

import java.net.URI;
import java.util.Map;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.lordkay.embabel.mcp.config.BioInsightProperties;

@Service
public class BioInsightApiClient {

    private final RestClient restClient;
    private final String baseUrl;

    public BioInsightApiClient(BioInsightProperties properties) {
        this.baseUrl = properties.getApiBaseUrl().replaceAll("/$", "");
        this.restClient = RestClient.builder().baseUrl(this.baseUrl).build();
    }

    public String get(String path) {
        return get(path, Map.of());
    }

    public String get(String path, Map<String, String> queryParams) {
        UriComponentsBuilder builder =
                UriComponentsBuilder.fromHttpUrl(baseUrl).path(normalizePath(path));
        queryParams.forEach(builder::queryParam);
        URI uri = builder.build(true).toUri();

        try {
            return restClient
                    .get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                        String body = new String(response.getBody().readAllBytes());
                        throw new BioInsightApiException(response.getStatusCode().value(), body);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        String body = new String(response.getBody().readAllBytes());
                        throw new BioInsightApiException(response.getStatusCode().value(), body);
                    })
                    .body(String.class);
        } catch (BioInsightApiException ex) {
            return errorJson(ex.status(), ex.body());
        } catch (Exception ex) {
            return errorJson(503, "Failed to reach BioInsight API at " + baseUrl + ": " + ex.getMessage());
        }
    }

    private static String normalizePath(String path) {
        return path.startsWith("/") ? path : "/" + path;
    }

    private static String errorJson(int status, String message) {
        String safe = message == null ? "" : message.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"error\":true,\"status\":" + status + ",\"detail\":\"" + safe + "\"}";
    }
}
