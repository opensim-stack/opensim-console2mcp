# opensim-console2mcp

Bridge for talking to the OpenSimulator REST console and streaming command output back to stdin/stdout.

## Build

```bash
mvn clean package
```

Build native executable (`target/opensim-console2mcp`):

```bash
mvn -Pnative-image -DskipTests package
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
