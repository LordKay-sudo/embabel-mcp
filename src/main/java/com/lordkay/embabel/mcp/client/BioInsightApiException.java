package com.lordkay.embabel.mcp.client;

public class BioInsightApiException extends RuntimeException {

    private final int status;
    private final String body;

    public BioInsightApiException(int status, String body) {
        super("BioInsight API " + status + ": " + body);
        this.status = status;
        this.body = body;
    }

    public int status() {
        return status;
    }

    public String body() {
        return body;
    }
}
