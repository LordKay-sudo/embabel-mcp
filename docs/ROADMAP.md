# embabel-mcp — implementation roadmap

**Repo:** [embabel-mcp](https://github.com/LordKay-sudo/embabel-mcp) (MCP on BioInsight `/api/v1`)  
**Depends on:** [bioinsight-graph ROADMAP](https://github.com/LordKay-sudo/bioinsight-graph/blob/main/docs/ROADMAP.md) for API/data  
**Optional:** [kg-rag-demo ROADMAP](https://github.com/LordKay-sudo/kg-rag-demo/blob/main/docs/ROADMAP.md)  
**Compact context:** [ECOSYSTEM_CONTEXT.md](https://github.com/LordKay-sudo/bioinsight-graph/blob/main/docs/ECOSYSTEM_CONTEXT.md)

Coupling rule: **HTTP to BioInsight only** — no monorepo requirement.

---

## Positioning

Agent tooling for the BioInsight product — not a second app. Value = **planned retrieval**, **provenance in every dossier**, **full-fidelity responses**, optional literature bridge. Move from static “step 1,2,3” prompts toward **adaptive investigation** (PRoH-style *process*, not their hypergraph store).

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

---

## P0 — Do next (blocked partly on BioInsight 1.x)

| ID | Task | Depends on | Done when |
|----|------|------------|-----------|
| **M1** | Tool/prompt: **`plan_investigation`** — JSON plan (entities, intent, tool sequence, stop rules) before dossier | — | Documented in USE_CASES; host can call plan then tools |
| **M2** | **Route by intent** in prompts: gene-first vs disease-first vs compare vs literature-needed | — | No one-size “always dossier” for every question |
| **M3** | **`resolve_identifier`** (symbol → ENSG, disease name → EFO/MONDO) | BioInsight 2.x IDs | Returns canonical id + ambiguity notes |
| **M4** | **`get_target_evidence`** — thin wrapper over API evidence breakdown | BioInsight 1.4 | Returns typed evidence list, not one score |
| **M5** | Dossier footer: **`data_version`** + canonical URLs from `/meta` | BioInsight 0.2 ✓ | Every workflow response cites version |
| **M6** | Prompt **`adaptive-gene-investigation`**: execute plan step; if missing ENSG/ambiguous disease → suggest next tool | M1, M3 | Described in USE_CASES |

---

## P1 — Provenance + fusion

| ID | Task | Done when |
|----|------|-----------|
| **M7** | **`export_provenance_bundle`** — JSON: meta, queries run, links, timestamps | Auditable export for HITL |
| **M8** | Refine **`graph-and-literature`**: graph first; call kg-rag **only** if sparse associations or user asks mechanism/lit | Conditional steps in prompt |
| **M9** | MCP resource **`bioinsight://investigation-playbook`** — when to use which tool/profile | One-page host guidance |
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

---

## Task pick order

1. **M5** (meta in footers — quick)  
2. **M1, M2, M6** (planning story — docs/prompts first, code optional)  
3. **M3, M4** after BioInsight **1.4 / 2.x**  
4. **M7, M8** (provenance + conditional RAG)  
5. **M10–M13**

---

*Living doc — link PRs to task IDs (e.g. `M4`).*
