# MCP context budget

How to keep Cursor/Claude sessions healthy as this server grows. Target: **stay under ~120k tokens** in the host conversation (hallucination risk rises when context is stuffed).

## Where tokens go

| Source | When loaded | Typical cost |
|--------|-------------|--------------|
| **Tool definitions** | Every MCP session start | ~200–800 tokens per tool |
| **Resources** | Only when the client reads a URI | Variable |
| **Tool results** | Each tool call | **Largest risk** (JSON subgraphs, long markdown) |
| **Prompts** | Listed at start; body when used | Moderate |
| **Your chat + code** | Ongoing | Dominates over time |

The server controls **tool count** and **per-response size**. The IDE controls **how many MCP servers** you attach.

## Server settings (`application.yml`)

```yaml
bioinsight:
  mcp:
    tool-profile: standard   # minimal | standard | full
    max-response-chars: 16000
    default-disease-limit: 10
```

Environment overrides: `BIOINSIGHT_MCP_TOOL_PROFILE`, `BIOINSIGHT_MCP_MAX_RESPONSE_CHARS`, `BIOINSIGHT_MCP_DEFAULT_DISEASE_LIMIT`.

### Tool profiles

| Profile | Tools (approx.) | Use when |
|---------|-----------------|----------|
| **minimal** | 5 core + 4 resources | Daily dev; smallest MCP footprint |
| **standard** | +5 entity/compare tools + prompts | Default |
| **full** | +neighbors, subgraph export, investigate | Debugging only |

**minimal** exposes: `bioinsight_health`, `bioinsight_stats`, `search_genes`, `search_diseases`, `build_target_dossier`.

**Disabled in minimal:** granular `get_*`, `compare_genes`, `export_gene_subgraph`, MCP prompts, ecosystem/HITL resources.

Optional bridges stay off unless enabled: `KG_RAG_ENABLED`, `SPECIALISED_SEARCH_ENABLED` (if added).

## Per-response cap

Every tool result is passed through `McpResponseCompactor` (default **16 000 characters** ≈ ~4k tokens). Oversized output is truncated with a short footer telling the model to narrow the query.

Worst offenders:

- `export_gene_subgraph` with `format=json` — **full profile only**
- Large `limit` on disease lists
- Chaining many granular tools instead of one `build_target_dossier`

## Client-side habits (Cursor)

1. **One biodata MCP server** for BioInsight — do not stack five unrelated MCPs in the same chat.
2. Prefer **`build_target_dossier`** over 4–6 separate `get_*` calls.
3. Default to **`format=markdown`** (already the default when omitted).
4. Read **`bioinsight://meta`** once per investigation, not every turn.
5. Start a **new chat** when the thread is long and the model drifts.
6. Use **`BIOINSIGHT_MCP_TOOL_PROFILE=minimal`** in `.env` for routine work.

## Rough 120k token budget (rule of thumb)

| Budget | Tokens (approx.) |
|--------|------------------|
| System + tools (standard profile) | ~8–15k |
| Your code/docs in workspace | varies |
| **Reserve for tool results** | ≤ 40–60k |
| **Reserve for reasoning + reply** | ≤ 40k |

Rule: **≤ 10 dossier calls** or **≤ 30 small searches** per long session without starting fresh.

## Growing the server safely

- Add **composite tools**, not more thin proxies.
- Put reference text in **resources** (on-demand), not tool descriptions.
- Gate experimental integrations behind **`enabled=false`** flags.
- New modalities → new profile tier or separate MCP server name in Cursor.

## Related

- [USE_CASES.md](./USE_CASES.md)
- [HUMAN_IN_THE_LOOP.md](./HUMAN_IN_THE_LOOP.md)
