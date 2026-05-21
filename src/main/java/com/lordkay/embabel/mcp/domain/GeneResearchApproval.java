package com.lordkay.embabel.mcp.domain;

/**
 * Human approval gate for {@link com.lordkay.embabel.mcp.agent.GeneResearchAgent}.
 * Fields map to an Embabel WaitFor form when HITL is enabled.
 */
public record GeneResearchApproval(boolean approved, String reviewerNotes) {}
