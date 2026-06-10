# embabel-mcp — implementation roadmap

**Repo:** [embabel-mcp](https://github.com/LordKay-sudo/embabel-mcp) (MCP on BioInsight `/api/v1`)  
**Depends on:** [bioinsight-graph ROADMAP](https://github.com/LordKay-sudo/bioinsight-graph/blob/main/docs/ROADMAP.md) for API/data  
**Optional:** [kg-rag-demo ROADMAP](https://github.com/LordKay-sudo/kg-rag-demo/blob/main/docs/ROADMAP.md)  
**Compact context:** [ECOSYSTEM_CONTEXT.md](https://github.com/LordKay-sudo/bioinsight-graph/blob/main/docs/ECOSYSTEM_CONTEXT.md)

Coupling rule: **HTTP to BioInsight only** — no monorepo requirement.

---

## Positioning

Agent tooling for the BioInsight product — not a second app. Value = **planned retrieval**, **provenance in every dossier**, **full-fidelity responses**, optional literature bridge. Move from static “step 1,2,3” prompts toward **adaptive investigation** (PRoH-style *process*, not their hypergraph store).

### Dual-channel retrieval (UniAI / production GraphRAG pattern)

| Channel | Source | When |
|---------|--------|------|
| **A — Local structured** | BioInsight API / `build_target_dossier` | Always first — scores, IDs, evidence |
| **B — Document** | kg-rag `ask` (cited chunks) | Only if channel A sparse, or user asks mechanism/literature (**M8**) |

Do not add a third “community summary” channel unless BioInsight graph is large enough to justify it.

---

## Shipped (baseline)

| Item |
|------|
| Core / extended / full **tool profiles** |
| `build_target_dossier`, provenance footers, `bioinsight://meta` |
| `compact-mode: off`, workflow tools never truncated |
| [RESPONSE_POLICY.md](./RESPONSE_POLICY.md), [CONTEXT_BUDGET.md](./CONTEXT_BUDGET.md), `bioinsight://context-policy` |
| MCP prompts: summarize, compare, disease targets, HITL, graph-and-literature |
| `GeneResearchAgent` / `research_gene` (Embabel) |
| **M1–M6** — `plan_investigation`, intent-routed prompts, `resolve_identifier`, `get_target_evidence`, meta source URLs in footers, `adaptive-gene-investigation`, `bioinsight://investigation-playbook` |

---

## P0 — Done (M1–M6)

| ID | Task | Status |
|----|------|--------|
| **M1** | **`plan_investigation`** — JSON plan before dossier | ✓ tool + USE_CASES |
| **M2** | **Route by intent** in prompts | ✓ gene / disease / compare / literature paths |
| **M3** | **`resolve_identifier`** | ✓ search-based; full ontology when BioInsight 2.x ships |
| **M4** | **`get_target_evidence`** | ✓ uses `/evidence` when present; else association scores |
| **M5** | Dossier footer: **`data_version`** + source URLs from `/meta` | ✓ |
| **M6** | **`adaptive-gene-investigation`** prompt | ✓ plan → resolve → branch |

---

## P1 — Provenance + fusion

| ID | Task | Done when |
|----|------|-----------|
| **M7** | **`export_provenance_bundle`** — JSON: meta, queries run, links, timestamps | Auditable export for HITL |
| **M8** | **`graph-and-literature` dual-channel**: channel A = dossier/API; channel B = kg-rag **only** if sparse associations, missing evidence types, or explicit literature question | Prompt + USE_CASES document stop rules |
| **M9** | MCP resource **`bioinsight://investigation-playbook`** — when to use which tool/profile | ✓ shipped with M2 |
| **M10** | README GIF: Cursor → dossier → BioInsight UI verify | Matches HITL story |

---

## P2 — Hardening

| ID | Task | Done when |
|----|------|-----------|
| **M11** | Integration test fixtures against frozen BioInsight subset | CI green without live Neo4j optional |
| **M12** | Log/tool metadata: `tool-profile`, response char count (for CONTEXT_BUDGET tuning) | Ops visibility |
| **M13** | Prompt: **public data → local API → MCP** tutorial (no Java required) | New contributor path |

---

## Tool profile guidance (keep defaults)

| Profile | Use |
|---------|-----|
| **minimal** | Daily Cursor — dossier + search + health |
| **standard** | Default — entity `get_*` + compare |
| **full** | Debug — neighbors, subgraph, `investigate_gene_symbol` |

Never use `compact-mode=truncate` for target–disease work. See [RESPONSE_POLICY.md](./RESPONSE_POLICY.md).

---

## Explicit non-goals

- Replacing BioInsight API or UI
- Unbounded wrappers to EBI Search / ChEMBL / sequence APIs (unless scoped later)
- Claiming “Knowledge Hypergraph RAG” without hypergraph backend
- Truncating `build_target_dossier` / investigate outputs
- Raw Neo4j Cypher MCP as default path (prefer stable `/api/v1` tools)
- Mandatory LangChain GraphRAG layer

## References (optional reading)

- [UniAI-GraphRAG — dual-channel + ontology-guided extract](https://arxiv.org/html/2603.25152v3) — fusion pattern for **M8**  
- [ML6 biomedical KG + Neo4j](https://blog.ml6.eu/accelerating-biomedical-knowledge-graph-construction-with-llms-db429952f4b2) — construction context for kg-rag, not BioInsight ingest  
- [Towards AI — Neo4j + LangChain GraphRAG](https://pub.towardsai.net/graphrag-explained-building-knowledge-grounded-llm-systems-with-neo4j-and-langchain-017a1820763e)

---

## Task pick order

1. **M7, M8** (provenance bundle + tighten dual-channel in host workflows)  
2. **M10–M13** (demo GIF, fixtures, ops logging, tutorial prompt)  
3. Revisit **M3/M4** when BioInsight **1.4 / 2.x** adds typed evidence and ontology resolve API

---

*Living doc — link PRs to task IDs (e.g. `M4`).*
