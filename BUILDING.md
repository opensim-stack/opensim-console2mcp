# Building

## Building Project

```bash
mvn clean package
```

Build native executable (`target/opensim-console2mcp`):

```bash
mvn -Pnative-image -DskipTests package
```

## Build Local Docker image

```bash
docker build -t opensim-console2mcp:local .
```

## Run local image

```bash
docker run --rm \
  -e CONSOLE_MCP_TRANSPORT=http \
  -e CONSOLE_MCP_HOST=0.0.0.0 \
  -e CONSOLE_MCP_PORT=8997 \
  -e OPENSIM_CONSOLE_URL=http://host.docker.internal:9000 \
  -e OPENSIM_CONSOLE_USER=ConsoleUser \
  -e OPENSIM_CONSOLE_PASS=ConsolePass \
  -p 8997:8997 \
  opensim-console2mcp:local
```

## Build and publish multiarch image

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