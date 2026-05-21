# Human-in-the-loop in embabel-mcp

See the full guide in [bioinsight-graph/docs/HUMAN_IN_THE_LOOP.md](https://github.com/LordKay-sudo/bioinsight-graph/blob/main/docs/HUMAN_IN_THE_LOOP.md).

## Quick reference

| Mode | Human intervention |
|------|-------------------|
| **BioInsight UI** | http://localhost:8080 — visual validation (always recommended) |
| **MCP prompt** | `review-gene-report` — Cursor user verifies before trusting the agent |
| **Embabel agent** | `bioinsight.hitl.enabled=true` → `WaitFor` form before final report |

Default MCP deployment: `bioinsight.hitl.enabled=false` (auto-approved) so SSE clients are not blocked.

## MCP resource

Read `bioinsight://human-in-the-loop` in MCP Inspector for this summary.
