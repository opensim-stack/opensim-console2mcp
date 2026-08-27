# opensim-console2mcp

[![Docker Hub](https://img.shields.io/badge/Docker%20Hub-bithatch%2Fopensim--console2mcp-2496ED?logo=docker&logoColor=white)](https://hub.docker.com/repository/docker/bithatch/opensim-console2mcp/general)

Bridges the OpenSimulator REST console and the MCP protocol. 

**For Issues And Discussions see main project [opensim-ai-docker](https://github.com/opensim-stack/opensim-ai-docker)**

*This is part of the [opensim-stack](https://opensim-stack.github.io/) and is intended to be used in conjunction with other parts of the stack. See [Docs](https://opensim-stack.github.io/docs/index.html) for full details.*

## Environment Variables

- `CONSOLE_MCP_TRANSPORT` (`http`, `sse`, or `stdio`)
- `CONSOLE_MCP_HOST`
- `CONSOLE_MCP_PORT`
- `OPENSIM_CONSOLE_URL`
- `OPENSIM_CONSOLE_USER`
- `OPENSIM_CONSOLE_PASS`

### Optional variables

- `CONSOLE_MCP_HTTP_ENDPOINT` (default `/mcp`)
- `CONSOLE_MCP_HTTP_BEARER_TOKEN`
- `CONSOLE_MCP_HTTP_KEEPALIVE_SECONDS`
- `CONSOLE_MCP_HTTP_DISALLOW_DELETE` (`true`/`false`)
- `CONSOLE_MCP_DIAGNOSTICS` (`true`/`false`)
- `OPENSIM_MCP_DEBUG` (`true`/`false`)
