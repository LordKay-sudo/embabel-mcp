# MCP response policy

How **embabel-mcp** handles tool output size. This project prioritises **complete biomedical evidence** over silent truncation.

## Lifecycle (never mid-processing)

Compaction and advisories run in `McpResponseCompactor.finish()` **after** a tool has:

1. Finished all internal HTTP calls to BioInsight (`BioInsightApiClient`)
2. Assembled markdown or JSON into one string
3. Returned that string to the MCP client (Cursor, Inspector, etc.)

```text
LLM calls tool
  → MCP handler runs (search → detail → diseases → neighbors → stats)
  → single String built
  → finish() applies policy once
  → full (or advised) string sent to LLM
```

**Not affected:** Embabel `GeneResearchAgent` internal `api.get()` calls — those never pass through `finish()`.

## Compact modes

| Mode | Default | Truncates content? | Advisory footer? |
|------|---------|-------------------|------------------|
| `off` | **yes** | No | Yes, if length > `warn-response-chars` |
| `warn` | no | No | Yes, when over threshold |
| `truncate` | no | Yes, for **standard** tools only | N/A |

Configure via `bioinsight.mcp.compact-mode` or `BIOINSIGHT_MCP_COMPACT_MODE`.

### Workflow exemption

These tools use `ResponseKind.workflow` and are **never truncated**, even when `compact-mode=truncate`:

- `build_target_dossier`
- `investigate_gene_symbol` (full profile only)

Controlled by `exempt-workflow-tools-from-truncation` (default `true`).

## Configuration reference

| Property / env | Default | Purpose |
|----------------|---------|---------|
| `tool-profile` / `BIOINSIGHT_MCP_TOOL_PROFILE` | `standard` | How many tools are registered at MCP connect |
| `compact-mode` / `BIOINSIGHT_MCP_COMPACT_MODE` | `off` | `off` \| `warn` \| `truncate` |
| `max-response-chars` / `BIOINSIGHT_MCP_MAX_RESPONSE_CHARS` | `0` | Hard cap when `truncate`; `0` = unlimited |
| `warn-response-chars` / `BIOINSIGHT_MCP_WARN_RESPONSE_CHARS` | `12000` | Character threshold for advisory footer |
| `default-disease-limit` / `BIOINSIGHT_MCP_DEFAULT_DISEASE_LIMIT` | `10` | Default `limit` in dossier / ranked endpoints |

Example `application.yml`:

```yaml
bioinsight:
  mcp:
    tool-profile: standard
    compact-mode: off
    max-response-chars: 0
    warn-response-chars: 12000
    exempt-workflow-tools-from-truncation: true
    default-disease-limit: 10
```

## Characters vs tokens

- The server measures **Java string length** (characters), not model tokens.
- Advisory footers include a **rough** estimate `chars / 4` — tokenizer varies by host model (Claude, GPT, etc.).
- A **120k-token conversation limit** is enforced by the **IDE + model**, not by `warn-response-chars` or `max-response-chars`.

See [CONTEXT_BUDGET.md](./CONTEXT_BUDGET.md) for how to stay within host context without losing data.

## Tool profiles

| Profile | Tools exposed |
|---------|----------------|
| **minimal** | `bioinsight_health`, `bioinsight_stats`, `search_genes`, `search_diseases`, `build_target_dossier` |
| **standard** | minimal + `get_gene`, `get_gene_diseases`, `get_disease`, `get_disease_genes`, `compare_genes` + MCP prompts |
| **full** | standard + `get_gene_neighbors`, `export_gene_subgraph`, `investigate_gene_symbol` |

Use **minimal** for routine Cursor sessions to reduce tool-schema tokens at connect time.

## Recommendations

| Do | Avoid |
|----|--------|
| `build_target_dossier` for investigations | Six separate `get_*` calls |
| `compact-mode: off` | `truncate` for production biodata work |
| `tool-profile: minimal` in long chats | `export_gene_subgraph` + `format=json` unless needed |
| New IDE chat when context is full | Expecting server to fix 120k host limit alone |

## MCP resource

Read `bioinsight://context-policy` in the MCP client for a short in-session summary (this document).

## Implementation

- `com.lordkay.embabel.mcp.format.McpResponseCompactor`
- `com.lordkay.embabel.mcp.config.McpContextProperties`
- `com.lordkay.embabel.mcp.tools.BioInsightToolSupport`

Tests: `McpResponseCompactorTest.java`
