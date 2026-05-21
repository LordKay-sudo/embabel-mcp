package com.lordkay.embabel.mcp.domain;

/**
 * Starting input for gene research — a HGNC-style symbol (e.g. BRCA1).
 */
public record GeneSymbolQuery(String symbol) {}
