# syntax=docker/dockerfile:1

FROM --platform=$BUILDPLATFORM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package dependency:copy-dependencies -DincludeScope=runtime

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /opt/opensim-console2mcp

COPY --from=build /workspace/target/opensim-console2mcp-*.jar /opt/opensim-console2mcp/app.jar
COPY --from=build /workspace/target/dependency /opt/opensim-console2mcp/lib
COPY docker/entrypoint.sh /usr/local/bin/opensim-console2mcp-entrypoint.sh
RUN chmod +x /usr/local/bin/opensim-console2mcp-entrypoint.sh

ENV CONSOLE_MCP_TRANSPORT=http \
    CONSOLE_MCP_HOST=0.0.0.0 \
    CONSOLE_MCP_PORT=8997 \
    CONSOLE_MCP_HTTP_ENDPOINT=/mcp \
    OPENSIM_CONSOLE_URL=http://opensim:9000 \
    OPENSIM_CONSOLE_USER=ConsoleUser \
    OPENSIM_CONSOLE_PASS=ConsolePass

EXPOSE 8997/tcp
ENTRYPOINT ["/usr/local/bin/opensim-console2mcp-entrypoint.sh"]
