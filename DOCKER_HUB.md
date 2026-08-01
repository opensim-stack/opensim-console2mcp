# opensim-console2mcp

`opensim-console2mcp` bridges the OpenSimulator REST console to MCP so AI agents and MCP clients can manage an OpenSim region using tools.

It is intended to be used as part of the **OpenSim Stack** project:
**"A docker stack to get an AI integrated virtual world up and running in minutes."**

## What This Image Does

- Connects to the OpenSimulator REST console
- Exposes OpenSim console operations through MCP over HTTP
- Works as an MCP sidecar for AI control and automation

## Quick Start

Run the container and point it at your OpenSim REST console:

```bash
docker run --rm \
  -e MCP_TRANSPORT=http \
  -e MCP_HOST=0.0.0.0 \
  -e MCP_PORT=9001 \
  -e MCP_HTTP_ENDPOINT=/mcp \
  -e OPENSIM_CONSOLE_URL=http://host.docker.internal:9000 \
  -e OPENSIM_CONSOLE_USER=ConsoleUser \
  -e OPENSIM_CONSOLE_PASS=ConsolePass \
  -p 9001:9001 \
  bithatch/opensim-console2mcp:latest
```

Then connect your MCP client to:

- `http://localhost:9001/mcp`

## Project Links

- Main AI Stack (`opensim-ai-docker`): https://github.com/opensim-stack/opensim-ai-docker
- `opensim-console2mcp` on GitHub: https://github.com/opensim-stack/opensim-console2mcp
- Related MCP server (`opensim-metaverse2mcp`):
  - GitHub: https://github.com/opensim-stack/opensim-metaverse2mcp
  - Docker Hub: https://hub.docker.com/repository/docker/bithatch/opensim-metaverse2mcp/general
