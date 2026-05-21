package com.lordkay.embabel.mcp.domain;

/**
 * Raw graph payloads gathered from BioInsight Graph API.
 */
public record GeneGraphBundle(
        String symbol,
        String geneId,
        String detailJson,
        String diseasesJson,
        String neighborsJson) {}
