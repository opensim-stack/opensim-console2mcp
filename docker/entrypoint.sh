#!/usr/bin/env sh
set -eu

transport="${MCP_TRANSPORT:-http}"
transport_lc=$(printf '%s' "$transport" | tr '[:upper:]' '[:lower:]')

mode=""
case "$transport_lc" in
  http)
    mode="HTTP"
    ;;
  sse)
    # Keep compatibility with the old opensim-mcp env contract.
    echo "[opensim-console2mcp] MCP_TRANSPORT=sse is not supported by this image, using HTTP streamable mode." >&2
    mode="HTTP"
    ;;
  stdio)
    mode="STDIO"
    ;;
  *)
    echo "[opensim-console2mcp] Unsupported MCP_TRANSPORT: $transport" >&2
    exit 1
    ;;
esac

set -- \
  --mode "$mode" \
  --username "${OPENSIM_CONSOLE_USER:-}" \
  --password "${OPENSIM_CONSOLE_PASS:-}"

if [ "${OPENSIM_MCP_DEBUG:-false}" = "true" ]; then
  set -- "$@" --debug
fi

if [ "${MCP_DIAGNOSTICS:-false}" = "true" ]; then
  set -- "$@" --mcp-diagnostics
fi

if [ "$mode" = "HTTP" ]; then
  set -- "$@" \
    --http-host "${MCP_HOST:-0.0.0.0}" \
    --http-port "${MCP_PORT:-8997}" \
    --http-endpoint "${MCP_HTTP_ENDPOINT:-/mcp}"

  if [ -n "${MCP_HTTP_KEEPALIVE_SECONDS:-}" ]; then
    set -- "$@" --http-keepalive-seconds "${MCP_HTTP_KEEPALIVE_SECONDS}"
  fi

  if [ "${MCP_HTTP_DISALLOW_DELETE:-false}" = "true" ]; then
    set -- "$@" --http-disallow-delete
  fi

  if [ -n "${MCP_HTTP_BEARER_TOKEN:-}" ]; then
    set -- "$@" --http-bearer-token "${MCP_HTTP_BEARER_TOKEN}"
  fi
fi

set -- "$@" "${OPENSIM_CONSOLE_URL:-http://opensim:9000}"

exec java -cp "/opt/opensim-console2mcp/app.jar:/opt/opensim-console2mcp/lib/*" \
  uk.co.bithatch.opensim.console2mcp.OpensimConsole2MCP \
  "$@"
