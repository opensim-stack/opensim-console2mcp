# opensim-console2mcp

Bridges the OpenSimulator REST console and the MCP protocol. Intend  for use as part of the [https://github.com/bithatch/opensim-osgrid-docker](Opensim and OSGrid Docker Stack)

## Build

```bash
mvn clean package
```

Build native executable (`target/opensim-console2mcp`):

```bash
mvn -Pnative-image -DskipTests package
```

## Docker (multiarch Java runtime image)

- `Dockerfile` (multi-stage Maven build, JVM runtime image)
- `docker/entrypoint.sh` (maps env vars to CLI arguments)

The container runs the regular JAR, not the native binary.

### Environment variables (compatible with opensim-osgrid-docker)

These are the same core variables used by the MCP sidecar in
`opensim-osgrid-docker/README.md`:

- `MCP_TRANSPORT` (`http`, `sse`, or `stdio`)
- `MCP_HOST`
- `MCP_PORT`
- `OPENSIM_CONSOLE_URL`
- `OPENSIM_CONSOLE_USER`
- `OPENSIM_CONSOLE_PASS`

Notes:

- `MCP_TRANSPORT=sse` is accepted for compatibility and currently runs HTTP streamable mode.
- For this image, `MCP_TRANSPORT=stdio` is mainly useful for local process testing (not typical container network usage).

Optional variables:

- `MCP_HTTP_ENDPOINT` (default `/mcp`)
- `MCP_HTTP_BEARER_TOKEN`
- `MCP_HTTP_KEEPALIVE_SECONDS`
- `MCP_HTTP_DISALLOW_DELETE` (`true`/`false`)
- `MCP_DIAGNOSTICS` (`true`/`false`)
- `OPENSIM_MCP_DEBUG` (`true`/`false`)

### Build local image

```bash
docker build -t opensim-console2mcp:local .
```

### Run local image

```bash
docker run --rm \
  -e MCP_TRANSPORT=http \
  -e MCP_HOST=0.0.0.0 \
  -e MCP_PORT=9001 \
  -e OPENSIM_CONSOLE_URL=http://host.docker.internal:9000 \
  -e OPENSIM_CONSOLE_USER=ConsoleUser \
  -e OPENSIM_CONSOLE_PASS=ConsolePass \
  -p 9001:9001 \
  opensim-console2mcp:local
```

### Build and publish multiarch image

Create/use a buildx builder once:

```bash
docker buildx create --name multiarch --use
docker buildx inspect --bootstrap
```

Build and push Linux AMD64 + ARM64:

```bash
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t bithatch/opensim-console2mcp:latest \
  -t bithatch/opensim-console2mcp:$(date +%Y%m%d) \
  --push \
  .
```

## Run

```bash
mvn exec:java -Dexec.mainClass=uk.co.bithatch.opensim.console2mcp.OpensimConsole2MCP -Dexec.args="--username ConsoleUser --password ConsolePass http://localhost:9000"
```

Console mode (existing stdin command loop):

```bash
mvn exec:java -Dexec.mainClass=uk.co.bithatch.opensim.console2mcp.OpensimConsole2MCP -Dexec.args="--mode CONSOLE --username ConsoleUser --password ConsolePass http://localhost:9000"
```

MCP stdio mode (for MCP clients):

```bash
mvn exec:java -Dexec.mainClass=uk.co.bithatch.opensim.console2mcp.OpensimConsole2MCP -Dexec.args="--mode STDIO --username ConsoleUser --password ConsolePass http://localhost:9000"
```

MCP streamable HTTP mode (SDK streamable transport):

```bash
mvn exec:java -Dexec.mainClass=uk.co.bithatch.opensim.console2mcp.OpensimConsole2MCP -Dexec.args="--mode HTTP --http-host 127.0.0.1 --http-port 8123 --http-endpoint /mcp --username ConsoleUser --password ConsolePass http://localhost:9000"
```

MCP streamable HTTP mode with bearer auth:

```bash
mvn exec:java -Dexec.mainClass=uk.co.bithatch.opensim.console2mcp.OpensimConsole2MCP -Dexec.args="--mode HTTP --http-port 8123 --http-endpoint /mcp --http-bearer-token secret-token --username ConsoleUser --password ConsolePass http://localhost:9000"
```

Debug and prompt detection:

```bash
mvn exec:java -Dexec.mainClass=uk.co.bithatch.opensim.console2mcp.OpensimConsole2MCP -Dexec.args="--debug --prompt-detection RELAXED --username ConsoleUser --password ConsolePass http://localhost:9000"
```

Dump rich help catalog (module -> commands -> arguments):

```bash
mvn exec:java -Dexec.mainClass=uk.co.bithatch.opensim.console2mcp.OpensimConsole2MCP -Dexec.args="--debug --dump-help-catalog --username ConsoleUser --password ConsolePass http://localhost:9000"
```

Write help catalog directly to a file:

```bash
mvn exec:java -Dexec.mainClass=uk.co.bithatch.opensim.console2mcp.OpensimConsole2MCP -Dexec.args="--catalog-file catalog.json --username ConsoleUser --password ConsolePass http://localhost:9000"
```

Then type OpenSim console commands, one per line. End input with `Ctrl-D`.

## Notes

- REST requests are sent as `POST` with `application/x-www-form-urlencoded` parameters.
- Polling uses `/ReadResponses/<SessionID>/` in a dedicated receiver thread with automatic retry.
- Debug logging is controlled by `--debug` (or `--no-debug`).
- `--mode` switches between `CONSOLE`, MCP `STDIO`, and MCP streamable `HTTP` behavior.
- HTTP mode serves MCP on `--http-endpoint` (default `/mcp`) with GET/POST/DELETE handled by MCP SDK streamable transport.
- If `--http-bearer-token` is set, requests must include `Authorization: Bearer <token>`.

## Local parser checks

```bash
mvn -q test-compile
java -cp target/classes:target/test-classes uk.co.bithatch.opensim.console2mcp.OpensimRESTConsoleParsingHarness
mvn -q -Dexec.classpathScope=test -Dexec.mainClass=uk.co.bithatch.opensim.console2mcp.OpensimMCPHttpHarness org.codehaus.mojo:exec-maven-plugin:3.6.2:java
```
