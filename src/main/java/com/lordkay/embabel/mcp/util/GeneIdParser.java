package com.lordkay.embabel.mcp.util;

public final class GeneIdParser {

    private GeneIdParser() {}

    /** First {@code "id":"ENSG..."} in a gene search JSON array. */
    public static String extractFirstGeneId(String searchJson) {
        int idx = searchJson.indexOf("\"id\":\"");
        if (idx < 0) {
            return null;
        }
        int start = idx + 6;
        int end = searchJson.indexOf('"', start);
        if (end < 0) {
            return null;
        }
        return searchJson.substring(start, end);
    }
}
