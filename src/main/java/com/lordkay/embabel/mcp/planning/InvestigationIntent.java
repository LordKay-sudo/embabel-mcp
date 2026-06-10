package com.lordkay.embabel.mcp.planning;

/** Routed investigation goal — drives tool sequence in {@link InvestigationPlanner}. */
public enum InvestigationIntent {
    GENE_TARGET,
    DISEASE_TARGETS,
    COMPARE_GENES,
    LITERATURE,
    GENERAL
}
