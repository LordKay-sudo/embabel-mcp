# MCP use cases

Worked examples for **embabel-mcp** against a running [BioInsight Graph](https://github.com/LordKay-sudo/bioinsight-graph) API. Default response format is **markdown**.

## 1. Target dossier (recommended handoff)

**Tool:** `build_target_dossier`  
**Input:** `symbol=BRCA1`  
**Output:** Gene detail, ranked diseases, neighborhood, graph stats, provenance footer, link to http://localhost:8080/gene/{id}

Use when you need one auditable report for a symbol.

## 2. Quick investigation

**Tool:** `investigate_gene_symbol`  
**Input:** `symbol=TP53`  

Same graph evidence as the dossier without the stats section — fewer tokens.

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

## Optional: literature (third repo)

Requires [kg-rag-demo](https://github.com/LordKay-sudo/kg-rag-demo) on :8001 and `KG_RAG_ENABLED=true`.  
**Prompt:** `graph-and-literature` — not required for BioInsight-only workflows.
