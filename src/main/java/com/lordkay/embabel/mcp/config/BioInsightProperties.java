package com.lordkay.embabel.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bioinsight")
public class BioInsightProperties {

    /**
     * Base URL of the BioInsight Graph FastAPI, e.g. http://localhost:8000/api/v1
     */
    private String apiBaseUrl = "http://localhost:8000/api/v1";

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }
}
