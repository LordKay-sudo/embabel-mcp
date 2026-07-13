package com.lordkay.embabel.mcp.domain;

/** HITL acknowledgment for GapForge dossier (does not approve individual L2 cards). */
public record GapResearchApproval(boolean approved, String reviewerNotes) {}
