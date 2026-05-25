# MCP use cases

Worked examples for **embabel-mcp** against a running [BioInsight Graph](https://github.com/LordKay-sudo/bioinsight-graph) API. Default response format is **markdown**.

**Response policy:** Full dossiers are never truncated (`BIOINSIGHT_MCP_COMPACT_MODE=off` by default). See [RESPONSE_POLICY.md](./RESPONSE_POLICY.md) and [CONTEXT_BUDGET.md](./CONTEXT_BUDGET.md).

## 1. Target dossier (recommended handoff)

**Tool:** `build_target_dossier`  
**Input:** `symbol=BRCA1`  
**Output:** Gene detail, ranked diseases, neighborhood, graph stats, provenance footer, link to http://localhost:8080/gene/{id}

Use when you need one auditable report for a symbol.

## 2. Quick investigation (full profile only)

**Tool:** `investigate_gene_symbol`  
**Input:** `symbol=TP53`  
**Requires:** `BIOINSIGHT_MCP_TOOL_PROFILE=full`

Same graph evidence as the dossier without the stats section. Prefer `build_target_dossier` in `minimal` or `standard` profiles.

## 3. Disease-centric targets

**Prompt:** `top-targets-for-disease`  
**Or tools:** `search_diseases` → `get_disease_genes`

Example disease query: `breast cancer`.

## 4. Compare two targets

**Tool:** `compare_genes` with `symbols=BRCA1,TP53`  
**Prompt:** `compare-gene-pair`

## 5. Live provenance

**Resource:** `bioinsight://meta`  
**API:** `GET http://localhost:8000/api/v1/meta`

Confirms `data_version`, disclaimer, and correlative-not-causal scope before trusting scores.

## 6. Human review

**Prompt:** `review-gene-report` after `research_gene` or `build_target_dossier`  
**UI:** Verify http://localhost:8080

## 7. Tool profiles and context

| Profile | When to use |
|---------|-------------|
| `minimal` | Daily Cursor use — smallest tool list at connect |
| `standard` | Default — adds entity `get_*` and `compare_genes` |
| `full` | Debugging — adds neighbors, subgraph export, `investigate_gene_symbol` |

Set `BIOINSIGHT_MCP_TOOL_PROFILE=minimal` in `.env`. Do **not** use `BIOINSIGHT_MCP_COMPACT_MODE=truncate` for target–disease work.

## Optional: literature (third repo)

Requires [kg-rag-demo](https://github.com/LordKay-sudo/kg-rag-demo) on :8001 and `KG_RAG_ENABLED=true`.  
**Prompt:** `graph-and-literature` — not required for BioInsight-only workflows.
