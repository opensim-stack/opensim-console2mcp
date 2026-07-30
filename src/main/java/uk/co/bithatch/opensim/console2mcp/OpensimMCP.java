package uk.co.bithatch.opensim.console2mcp;

import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;

public class OpensimMCP implements AutoCloseable {

    private final OpensimRESTConsole console;
    private final boolean diagnostics;
    private McpSyncServer server;
    private Server httpServer;

    public OpensimMCP(OpensimRESTConsole console) {
        this(console, false);
    }

    public OpensimMCP(OpensimRESTConsole console, boolean diagnostics) {
        this.console = console;
        this.diagnostics = diagnostics;
    }

    public void start() {
        startStdio();
    }

    public void startStdio() {
        var started = System.nanoTime();
        diag("startStdio() entered");
        var transportProvider = new StdioServerTransportProvider();
        diag("StdioServerTransportProvider created");
        var serverSpec = McpServer.sync(transportProvider)
                .requestTimeout(Duration.ofSeconds(60))
                .serverInfo("opensim-console2mcp", "0.0.1")
                .instructions("OpenSimulator REST console MCP bridge. Tools return command output as an array of lines.");

        diag("Loading full help catalog...");
        var catalogLoadStarted = System.nanoTime();
        var catalog = console.loadHelpCatalog();
        var moduleCount = catalog.modules().size();
        var commandCount = catalog.modules().stream().mapToInt(module -> module.commands().size()).sum();
        diag("Catalog loaded in " + millisSince(catalogLoadStarted) + "ms (modules=" + moduleCount + ", commands="
                + commandCount + ")");

        var toolSpecs = buildToolSpecifications(catalog);
        diag("Built " + toolSpecs.size() + " MCP tool specifications");
        serverSpec.tools(toolSpecs);
        diag("Building MCP server instance...");
        server = serverSpec.build();
        diag("MCP server built in " + millisSince(started) + "ms");
    }

    public String startHttp(String bindAddress, int port, String endpoint, Long keepAliveSeconds,
            boolean disallowDelete, String bearerToken) throws Exception {
        var started = System.nanoTime();
        var normalizedEndpoint = normalizeEndpoint(endpoint);
        diag("startHttp() entered host=" + bindAddress + " port=" + port + " endpoint=" + normalizedEndpoint);

        var transportBuilder = HttpServletStreamableServerTransportProvider.builder()
                .objectMapper(new ObjectMapper())
                .mcpEndpoint(normalizedEndpoint)
                .disallowDelete(disallowDelete);
        if (keepAliveSeconds != null && keepAliveSeconds > 0) {
            transportBuilder.keepAliveInterval(Duration.ofSeconds(keepAliveSeconds));
        }

        var transportProvider = transportBuilder.build();
        var serverSpec = McpServer.sync(transportProvider)
                .requestTimeout(Duration.ofSeconds(60))
                .serverInfo("opensim-console2mcp", "0.0.1")
                .instructions("OpenSimulator REST console MCP bridge. Tools return command output as an array of lines.");

        diag("Loading full help catalog...");
        var catalogLoadStarted = System.nanoTime();
        var catalog = console.loadHelpCatalog();
        var moduleCount = catalog.modules().size();
        var commandCount = catalog.modules().stream().mapToInt(module -> module.commands().size()).sum();
        diag("Catalog loaded in " + millisSince(catalogLoadStarted) + "ms (modules=" + moduleCount + ", commands="
                + commandCount + ")");

        var toolSpecs = buildToolSpecifications(catalog);
        diag("Built " + toolSpecs.size() + " MCP tool specifications");
        serverSpec.tools(toolSpecs);
        diag("Building MCP streamable server instance...");
        server = serverSpec.build();

        httpServer = createHttpServer(bindAddress, port, normalizedEndpoint, transportProvider, bearerToken);
        httpServer.start();

        var connector = (ServerConnector) httpServer.getConnectors()[0];
        var localHost = connector.getHost() == null ? "0.0.0.0" : connector.getHost();
        var advertisedUrl = "http://" + localHost + ":" + connector.getLocalPort() + normalizedEndpoint;
        diag("MCP HTTP server started at " + advertisedUrl + " in " + millisSince(started) + "ms");
        return advertisedUrl;
    }

    public void runUntilInterrupted() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void close() {
        if (httpServer != null) {
            try {
                httpServer.stop();
                diag("HTTP server stopped.");
            } catch (Exception e) {
                diag("Error stopping HTTP server: " + e.getMessage());
            }
        }
        if (server != null) {
            server.closeGracefully();
            server.close();
        }
    }

    static String normalizeEndpoint(String endpoint) {
        var value = endpoint == null ? "" : endpoint.trim();
        if (value.isEmpty()) {
            return "/mcp";
        }
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        while (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    static boolean isAuthorizedBearerHeader(String header, String expectedToken) {
        if (expectedToken == null || expectedToken.isBlank()) {
            return true;
        }
        if (header == null || header.isBlank()) {
            return false;
        }
        var prefix = "Bearer ";
        if (!header.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return false;
        }
        var presentedToken = header.substring(prefix.length()).trim();
        return expectedToken.equals(presentedToken);
    }

    private static Server createHttpServer(String bindAddress, int port, String endpoint,
            HttpServletStreamableServerTransportProvider provider, String bearerToken) {
        var server = new Server();
        var connector = new ServerConnector(server);
        connector.setHost(bindAddress);
        connector.setPort(port);
        server.addConnector(connector);

        var context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(provider), endpoint);

        if (bearerToken != null && !bearerToken.isBlank()) {
            var holder = new FilterHolder(new BearerTokenAuthFilter(bearerToken));
            context.addFilter(holder, endpoint, EnumSet.of(DispatcherType.REQUEST, DispatcherType.ASYNC));
        }

        server.setHandler(context);
        return server;
    }

    private static final class BearerTokenAuthFilter implements Filter {

        private final String expectedToken;

        private BearerTokenAuthFilter(String expectedToken) {
            this.expectedToken = expectedToken;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws java.io.IOException, ServletException {
            if (!(request instanceof HttpServletRequest httpRequest) || !(response instanceof HttpServletResponse httpResponse)) {
                chain.doFilter(request, response);
                return;
            }

            var header = httpRequest.getHeader("Authorization");
            if (!isAuthorizedBearerHeader(header, expectedToken)) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setHeader("WWW-Authenticate", "Bearer");
                httpResponse.setContentType("application/json");
                httpResponse.setCharacterEncoding("UTF-8");
                httpResponse.getWriter().write("{\"error\":\"Unauthorized\"}");
                return;
            }

            chain.doFilter(request, response);
        }
    }

    private List<McpServerFeatures.SyncToolSpecification> buildToolSpecifications(OpensimRESTConsole.HelpCatalog catalog) {
        var specs = new ArrayList<McpServerFeatures.SyncToolSpecification>();
        var usedNames = new LinkedHashSet<String>();
        for (var module : catalog.modules()) {
            for (var command : module.commands()) {
                var toolName = uniqueToolName(module.name(), command.name(), usedNames);
                var tool = McpSchema.Tool.builder()
                        .name(toolName)
                        .description(module.name() + ": " + command.description())
                        .inputSchema(buildInputSchema(command))
                        .build();
                specs.add(new McpServerFeatures.SyncToolSpecification(tool,
                        (exchange, args) -> callTool(command, args)));
            }
        }
        return specs;
    }

    private McpSchema.CallToolResult callTool(OpensimRESTConsole.HelpCommand command, Map<String, Object> args) {
        var commandLine = renderCommandLine(command, args == null ? Map.of() : args);
        try {
            var started = System.nanoTime();
            diag("Tool call start: " + commandLine);
            var lines = console.execute(commandLine).toList();
            var envelope = toResultEnvelope(commandLine, true, lines, null);
            diag("Tool call success in " + millisSince(started) + "ms, lines=" + lines.size());
            return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(envelope)), false);
        } catch (Exception e) {
            var message = e.getMessage() == null ? "Unknown error" : e.getMessage();
            diag("Tool call error: " + commandLine + " -> " + message);
            var envelope = toResultEnvelope(commandLine, false, List.of(), message);
            return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(envelope)), true);
        }
    }

    private String renderCommandLine(OpensimRESTConsole.HelpCommand command, Map<String, Object> args) {
        var out = new StringBuilder(command.name());
        for (var argument : command.arguments()) {
            var value = args.get(argument.name());
            if (value == null) {
                continue;
            }

            if ("option".equals(argument.kind())) {
                var optionSwitch = extractOptionSwitch(argument);
                if (argument.option() == null) {
                    if (isTruthy(value)) {
                        out.append(' ').append(optionSwitch);
                    }
                } else {
                    out.append(' ').append(optionSwitch).append(' ').append(quote(shellString(value)));
                }
            } else {
                out.append(' ').append(quote(shellString(value)));
            }
        }
        return out.toString();
    }

    private McpSchema.JsonSchema buildInputSchema(OpensimRESTConsole.HelpCommand command) {
        var properties = new LinkedHashMap<String, Object>();
        var required = new ArrayList<String>();

        for (var argument : command.arguments()) {
            var prop = new LinkedHashMap<String, Object>();
            var isFlag = "option".equals(argument.kind()) && argument.option() == null;
            prop.put("type", isFlag ? "boolean" : "string");
            prop.put("description", argument.token());
            if (!argument.values().isEmpty()) {
                prop.put("enum", argument.values());
            }
            if (argument.option() != null) {
                prop.put("option", argument.option());
            }
            properties.put(argument.name(), prop);

            if (!argument.optional()) {
                required.add(argument.name());
            }
        }

        return new McpSchema.JsonSchema("object", properties, required, false, Map.of(), Map.of());
    }

    private static String uniqueToolName(String moduleName, String commandName, Set<String> used) {
        var base = sanitizeToolName(moduleName + "_" + commandName);
        var candidate = base;
        var counter = 2;
        while (used.contains(candidate)) {
            candidate = base + "_" + counter;
            counter++;
        }
        used.add(candidate);
        return candidate;
    }

    private static String sanitizeToolName(String input) {
        var lowered = input.toLowerCase();
        var out = new StringBuilder();
        var previousUnderscore = false;
        for (int i = 0; i < lowered.length(); i++) {
            var c = lowered.charAt(i);
            var valid = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-';
            if (valid) {
                out.append(c);
                previousUnderscore = false;
            } else if (!previousUnderscore) {
                out.append('_');
                previousUnderscore = true;
            }
        }
        var result = out.toString().replaceAll("^_+|_+$", "");
        return result.isBlank() ? "tool" : result;
    }

    private static String extractOptionSwitch(OpensimRESTConsole.HelpArgument argument) {
        var token = argument.token();
        if (token.startsWith("[") && token.endsWith("]")) {
            token = token.substring(1, token.length() - 1).trim();
        }
        var candidates = token.split("\\|");
        var chosen = candidates[candidates.length - 1].trim();
        var equals = chosen.indexOf('=');
        if (equals > 0) {
            chosen = chosen.substring(0, equals);
        }
        if (!chosen.startsWith("-")) {
            return "--" + argument.name();
        }
        return chosen;
    }

    private static boolean isTruthy(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(shellString(value));
    }

    private static String shellString(Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return value == null ? "" : String.valueOf(value);
    }

    private static String quote(String value) {
        if (value.isEmpty()) {
            return "\"\"";
        }
        var needsQuoting = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isWhitespace(c) || c == '"') {
                needsQuoting = true;
                break;
            }
        }
        if (!needsQuoting) {
            return value;
        }
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private static String toResultEnvelope(String command, boolean ok, List<String> lines, String error) {
        var out = new StringBuilder();
        out.append("{");
        out.append("\"ok\":").append(ok).append(',');
        out.append("\"command\":\"").append(escapeJson(command)).append("\",");
        out.append("\"lineCount\":").append(lines.size()).append(',');
        out.append("\"lines\":").append(toJsonArray(lines));
        if (error != null && !error.isBlank()) {
            out.append(',').append("\"error\":\"").append(escapeJson(error)).append("\"");
        }
        out.append("}");
        return out.toString();
    }

    private static String toJsonArray(List<String> lines) {
        var out = new StringBuilder("[");
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            out.append('"').append(escapeJson(lines.get(i))).append('"');
        }
        out.append(']');
        return out.toString();
    }

    private static String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private void diag(String message) {
        if (!diagnostics) {
            return;
        }
        System.err.println("[OpensimMCP][DIAG][" + java.time.Instant.now() + "] " + message);
    }

    private static long millisSince(long startedAtNanos) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos);
    }
}