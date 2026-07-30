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
- `--mode` switches between `CONSOLE` and MCP `STDIO` server behavior.

## Local parser checks

```bash
mvn -q test-compile
java -cp target/classes:target/test-classes uk.co.bithatch.opensim.console2mcp.OpensimRESTConsoleParsingHarness
```
