package uk.co.bithatch.opensim.console2mcp;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;

public final class OpensimMCPHttpHarness {

    public static void main(String[] args) {
        testEndpointNormalization();
        testBearerAuthorizationParsing();
        testTransportBuilderObjectMapperRequirement();
        System.out.println("All MCP HTTP checks passed.");
    }

    private static void testEndpointNormalization() {
        require("/mcp".equals(OpensimMCP.normalizeEndpoint(null)), "null endpoint should default to /mcp");
        require("/mcp".equals(OpensimMCP.normalizeEndpoint("mcp")), "leading slash should be added");
        require("/mcp".equals(OpensimMCP.normalizeEndpoint("/mcp/")), "trailing slash should be trimmed");
        require("/custom/path".equals(OpensimMCP.normalizeEndpoint("custom/path/")), "nested endpoint normalization failed");
    }

    private static void testBearerAuthorizationParsing() {
        require(OpensimMCP.isAuthorizedBearerHeader("Bearer abc", "abc"), "exact bearer token should pass");
        require(OpensimMCP.isAuthorizedBearerHeader("bearer abc", "abc"), "case-insensitive Bearer prefix should pass");
        require(!OpensimMCP.isAuthorizedBearerHeader("Bearer wrong", "abc"), "mismatched token should fail");
        require(!OpensimMCP.isAuthorizedBearerHeader(null, "abc"), "missing header should fail when token expected");
        require(OpensimMCP.isAuthorizedBearerHeader(null, ""), "empty expected token should disable auth");
    }

    private static void testTransportBuilderObjectMapperRequirement() {
        var provider = HttpServletStreamableServerTransportProvider.builder()
                .objectMapper(new ObjectMapper())
                .mcpEndpoint("/mcp")
                .build();
        require(provider != null, "expected streamable provider instance");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
