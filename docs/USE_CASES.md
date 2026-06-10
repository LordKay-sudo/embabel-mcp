# MCP use cases

Worked examples for **embabel-mcp** against a running [BioInsight Graph](https://github.com/LordKay-sudo/bioinsight-graph) API. Default response format is **markdown**.

**Response policy:** Full dossiers are never truncated (`BIOINSIGHT_MCP_COMPACT_MODE=off` by default). See [RESPONSE_POLICY.md](./RESPONSE_POLICY.md) and [CONTEXT_BUDGET.md](./CONTEXT_BUDGET.md).

## 1. Plan then investigate (M1 + M6)

**Tools:** `plan_investigation` → `resolve_identifier` → intent-specific tools  
**Prompt:** `adaptive-gene-investigation`  
**Input:** `question=What are the top disease associations for BRCA1?`

`plan_investigation` returns JSON: intent, entities, tool_sequence, stop_rules.  
If `resolve_identifier` sets `ambiguous: true`, stop and ask the user — do not run `build_target_dossier` yet.

Resource: `bioinsight://investigation-playbook`

## 2. Target dossier (recommended handoff)

**Tool:** `build_target_dossier`  
**Input:** `symbol=BRCA1`  
**Output:** Gene detail, ranked diseases, neighborhood, graph stats, provenance footer (data_version + source URLs), link to http://localhost:8080/gene/{id}

Use when you need one auditable report for a symbol. Prefer after `plan_investigation` with intent `GENE_TARGET`.

## 3. Resolve identifier (M3)

**Tool:** `resolve_identifier`  
**Input:** `query=BRCA1`, `entityType=gene`  
**Requires:** `BIOINSIGHT_MCP_TOOL_PROFILE=standard` or `full`

Returns `canonical_id`, `id_system`, and `candidates[]` when ambiguous.

## 4. Target evidence (M4)

**Tool:** `get_target_evidence`  
**Input:** `geneIdOrSymbol=BRCA1`, optional `diseaseId`  
**Requires:** standard or full profile

Uses BioInsight `/genes/{id}/evidence` when available; otherwise association scores with a clear note.

## 5. Quick investigation (full profile only)

**Tool:** `investigate_gene_symbol`  
**Input:** `symbol=TP53`  
**Requires:** `BIOINSIGHT_MCP_TOOL_PROFILE=full`

Same graph evidence as the dossier without the stats section. Prefer `build_target_dossier` in `minimal` or `standard` profiles.

## 6. Disease-centric targets (M2 disease-first)

**Prompt:** `top-targets-for-disease`  
**Or tools:** `search_diseases` → `get_disease_genes`

Example disease query: `breast cancer`.

## 7. Compare two targets (M2 compare path)

**Tool:** `compare_genes` with `symbols=BRCA1,TP53`  
**Prompt:** `compare-gene-pair`

## 8. Live provenance

**Resource:** `bioinsight://meta`  
**API:** `GET http://localhost:8000/api/v1/meta`

Confirms `data_version`, disclaimer, and correlative-not-causal scope before trusting scores.

## 9. Human review

**Prompt:** `review-gene-report` after `research_gene` or `build_target_dossier`  
**UI:** Verify http://localhost:8080

## 10. Tool profiles and context

| Profile | When to use |
|---------|-------------|
| `minimal` | Daily Cursor — `plan_investigation`, dossier, search, health |
| `standard` | Default — adds `resolve_identifier`, `get_target_evidence`, entity `get_*`, `compare_genes` |
| `full` | Debugging — adds neighbors, subgraph export, `investigate_gene_symbol` |

Set `BIOINSIGHT_MCP_TOOL_PROFILE=minimal` in `.env`. Do **not** use `BIOINSIGHT_MCP_COMPACT_MODE=truncate` for target–disease work.

## Optional: literature (third repo)

Requires [kg-rag-demo](https://github.com/LordKay-sudo/kg-rag-demo) on :8001 and `KG_RAG_ENABLED=true`.  
**Prompt:** `graph-and-literature` — not required for BioInsight-only workflows.
