package uk.co.bithatch.opensim.console2mcp;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "opensim-console2mcp", mixinStandardHelpOptions = true, description = "Interactive bridge to the OpenSimulator REST console.")
public class OpensimConsole2MCP implements Callable<Integer> {

	public enum Mode {
		STDIO,
		HTTP,
		CONSOLE
	}
	
	private static final String DEFAULT_URL = "http://localhost:9000";

	@Option(names = { "-u", "--username" }, description = "Console username")
	private String username;

	@Option(names = { "-p", "--password" }, description = "Console password")
	private String password;

	@Option(names = "--debug", negatable = true, description = "Enable verbose transport debug logging (default: ${DEFAULT-VALUE})")
	private boolean debug;

	@Option(names = "--prompt-detection", description = "Prompt detection mode: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
	private OpensimRESTConsole.PromptDetectionMode promptDetection = OpensimRESTConsole.PromptDetectionMode.RELAXED;

	@Option(names = "--dump-help-catalog", description = "Fetch and print parsed help for all modules as JSON, then exit")
	private boolean dumpHelpCatalog;

	@Option(names = "--catalog-file", description = "Write help catalog JSON to this file and exit")
	private Path catalogFile;

	@Option(names = "--mode", description = "Run mode: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE})")
	private Mode mode = Mode.CONSOLE;

	@Option(names = "--mcp-diagnostics", description = "Enable MCP startup/handshake diagnostics to stderr")
	private boolean mcpDiagnostics;

	@Option(names = "--http-host", description = "HTTP bind address for MCP streamable mode (default: ${DEFAULT-VALUE})")
	private String httpHost = "127.0.0.1";

	@Option(names = "--http-port", description = "HTTP bind port for MCP streamable mode (default: ${DEFAULT-VALUE})")
	private int httpPort = 8123;

	@Option(names = "--http-endpoint", description = "HTTP endpoint path for MCP streamable mode (default: ${DEFAULT-VALUE})")
	private String httpEndpoint = "/mcp";

	@Option(names = "--http-keepalive-seconds", description = "Optional server keepalive interval in seconds for streamable mode")
	private Long httpKeepAliveSeconds;

	@Option(names = "--http-disallow-delete", negatable = true, description = "Allow DELETE session requests on streamable endpoint (default: ${DEFAULT-VALUE})")
	private boolean httpDisallowDelete;

	@Option(names = "--http-bearer-token", description = "Require Authorization: Bearer <token> in HTTP MCP mode")
	private String httpBearerToken;

	@Parameters(index = "0", arity="0..1", description = "REST console URL")
	private Optional<String> url;

	public static void main(String[] args) {
		var app = new OpensimConsole2MCP();
		var exitCode = new CommandLine(app).execute(args);
		System.exit(exitCode);
	}

	@Override
	public Integer call() {
		var user = Optional.ofNullable(username);
		var pass = Optional.ofNullable(password).map(String::toCharArray);

		try (var opensim = new OpensimRESTConsole(url.orElse(DEFAULT_URL), user, pass, debug, promptDetection)) {
			if (dumpHelpCatalog || catalogFile != null) {
				var json = toJson(opensim.loadHelpCatalog());
				if (catalogFile != null) {
					Files.writeString(catalogFile, json, StandardCharsets.UTF_8);
					System.err.println("Wrote help catalog to " + catalogFile.toAbsolutePath());
				}
				if (dumpHelpCatalog || catalogFile == null) {
					System.out.println(json);
				}
				return 0;
			}

			if (mode == Mode.STDIO) {
				if (mcpDiagnostics) {
					System.err.println("[OpensimMCP][DIAG] Entering STDIO mode.");
				}
				try (var mcp = new OpensimMCP(opensim, mcpDiagnostics)) {
					mcp.startStdio();
					if (mcpDiagnostics) {
						System.err.println("[OpensimMCP][DIAG] MCP start() completed; waiting for client messages.");
					}
					mcp.runUntilInterrupted();
				}
				return 0;
			}

			if (mode == Mode.HTTP) {
				if (mcpDiagnostics) {
					System.err.println("[OpensimMCP][DIAG] Entering HTTP streamable mode.");
				}
				try (var mcp = new OpensimMCP(opensim, mcpDiagnostics)) {
					var endpoint = mcp.startHttp(httpHost, httpPort, httpEndpoint, httpKeepAliveSeconds,
							httpDisallowDelete, httpBearerToken);
					System.err.println("MCP streamable endpoint ready at " + endpoint);
					if (httpBearerToken != null && !httpBearerToken.isBlank()) {
						System.err.println("HTTP auth enabled (Bearer token required).");
					}
					mcp.runUntilInterrupted();
				}
				return 0;
			}

			return runConsoleMode(opensim);
		} catch (Exception e) {
			System.err.println("Failed: " + e.getMessage());
			if (debug) {
				e.printStackTrace(System.err);
			}
			return 1;
		}
	}

	private static Integer runConsoleMode(OpensimRESTConsole opensim) throws Exception {
		try (var reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
			String line;
			System.out.print("> ");
			while ((line = reader.readLine()) != null) {
				if (line.isBlank()) {
					continue;
				}
				opensim.execute(line).forEach(System.out::println);
				System.out.print("> ");
			}
			return 0;
		}
	}

	private static String toJson(OpensimRESTConsole.HelpCatalog catalog) {
		var out = new StringBuilder();
		out.append("{\n  \"modules\": [\n");
		var modules = catalog.modules();
		for (int i = 0; i < modules.size(); i++) {
			var module = modules.get(i);
			out.append("    {\n");
			out.append("      \"name\": \"").append(escapeJson(module.name())).append("\",\n");
			out.append("      \"commands\": [\n");
			var commands = module.commands();
			for (int j = 0; j < commands.size(); j++) {
				var command = commands.get(j);
				out.append("        {\n");
				out.append("          \"name\": \"").append(escapeJson(command.name())).append("\",\n");
				out.append("          \"signature\": \"").append(escapeJson(command.signature())).append("\",\n");
				out.append("          \"description\": \"").append(escapeJson(command.description())).append("\",\n");
				out.append("          \"arguments\": [\n");
				var arguments = command.arguments();
				for (int k = 0; k < arguments.size(); k++) {
					var argument = arguments.get(k);
					out.append("            {\"token\": \"").append(escapeJson(argument.token())).append("\", ");
					out.append("\"name\": \"").append(escapeJson(argument.name())).append("\", ");
					out.append("\"optional\": ").append(argument.optional()).append(", ");
					out.append("\"kind\": \"").append(escapeJson(argument.kind())).append("\", ");
					out.append("\"aliases\": ").append(stringArrayJson(argument.aliases())).append(", ");
					out.append("\"values\": ").append(stringArrayJson(argument.values())).append(", ");
					out.append("\"option\": ").append(nullableStringJson(argument.option())).append("}");
					if (k < arguments.size() - 1) {
						out.append(',');
					}
					out.append('\n');
				}
				out.append("          ]\n");
				out.append("        }");
				if (j < commands.size() - 1) {
					out.append(',');
				}
				out.append('\n');
			}
			out.append("      ]\n");
			out.append("    }");
			if (i < modules.size() - 1) {
				out.append(',');
			}
			out.append('\n');
		}
		out.append("  ]\n}");
		return out.toString();
	}

	private static String escapeJson(String text) {
		if (text == null) {
			return "";
		}
		return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
	}

	private static String stringArrayJson(Iterable<String> values) {
		var out = new StringBuilder("[");
		var first = true;
		for (var value : values) {
			if (!first) {
				out.append(", ");
			}
			out.append("\"").append(escapeJson(value)).append("\"");
			first = false;
		}
		out.append(']');
		return out.toString();
	}

	private static String nullableStringJson(String value) {
		if (value == null) {
			return "null";
		}
		return "\"" + escapeJson(value) + "\"";
	}
}