# MCP context budget

How to keep IDE sessions healthy without **cutting biomedical tool output**. The host model’s **120k-token window** (varies by model) is separate from this server’s defaults.

Technical detail: [RESPONSE_POLICY.md](./RESPONSE_POLICY.md) · MCP resource `bioinsight://context-policy`

## Characters ≠ tokens (important)

| Measure | What it is | Typical scale |
|---------|------------|---------------|
| **16 000 characters** | ~4k–5k tokens *if* you use the rough rule ~4 chars/token | One medium tool result |
| **120 000 tokens** | Entire conversation budget in Cursor/Claude | System + tools + all messages + all tool results |

Token counts **depend on the model tokenizer** (Claude, GPT-4, Gemini, etc. differ). This server does not count tokens; it only measures **Java string length** for optional advisories.

**There is no setting that maps 16k chars → 120k tokens.** Tool profiles reduce **startup** tool definitions; advisories remind you when a single result is large.

## Design principle: full fidelity by default

Disease–target evidence must not be silently dropped. Compaction runs **only after** a tool has finished all internal API calls and assembled one response — never mid-request.

| Policy | Default | Behaviour |
|--------|---------|-----------|
| **compact-mode: off** | yes | Return full body; optional advisory footer if very large |
| **compact-mode: warn** | no | Full body + always show size advisory when over threshold |
| **compact-mode: truncate** | no | Hard cap (opt-in); **never** applied to workflow dossiers |

Workflow tools (`build_target_dossier`, `investigate_gene_symbol`) are **always exempt** from truncation.

## Server settings

```yaml
bioinsight:
  mcp:
    tool-profile: standard          # minimal | standard | full
    compact-mode: off               # off | warn | truncate
    max-response-chars: 0           # only if compact-mode=truncate (0 = unlimited)
    warn-response-chars: 12000      # advisory footer threshold (chars, not tokens)
    exempt-workflow-tools-from-truncation: true
```

Environment: `BIOINSIGHT_MCP_TOOL_PROFILE`, `BIOINSIGHT_MCP_COMPACT_MODE`, `BIOINSIGHT_MCP_MAX_RESPONSE_CHARS`, `BIOINSIGHT_MCP_WARN_RESPONSE_CHARS`.

## What actually saves context (ranked)

1. **`tool-profile=minimal`** — fewer tools registered at MCP connect (~largest server-side win).
2. **One `build_target_dossier` per gene** — not six granular `get_*` calls.
3. **Avoid `export_gene_subgraph` + json** in long chats (full profile only).
4. **Fewer MCP servers** attached in Cursor.
5. **New chat** when the thread is long — host-side, not server-side.
6. **`compact-mode=truncate`** — emergency only; not recommended for this project.

## Rough 120k token budget (host / model dependent)

| Piece | Tokens (very rough) |
|-------|---------------------|
| System + MCP tool schemas (standard profile) | 8–15k |
| Workspace files (if in context) | varies |
| Each `build_target_dossier` (full, not truncated) | 2–8k |
| Your messages + model replies | rest |

Because tokenization is model-specific, treat this table as order-of-magnitude only.

## Client habits (Cursor)

1. Prefer **`build_target_dossier`** for investigations.
2. Use **`format=markdown`** (default).
3. **`BIOINSIGHT_MCP_TOOL_PROFILE=minimal`** for routine work.
4. Do not enable **`BIOINSIGHT_MCP_COMPACT_MODE=truncate`** unless debugging transport limits.

## Related

- [RESPONSE_POLICY.md](./RESPONSE_POLICY.md) — when `finish()` runs, workflow exemption, config table
- [USE_CASES.md](./USE_CASES.md)
- [HUMAN_IN_THE_LOOP.md](./HUMAN_IN_THE_LOOP.md)
