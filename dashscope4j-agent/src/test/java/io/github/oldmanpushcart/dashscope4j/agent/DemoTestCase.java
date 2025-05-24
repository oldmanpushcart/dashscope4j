package io.github.oldmanpushcart.dashscope4j.agent;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class DemoTestCase {

    static final String AMAP_AK = System.getenv("AMAP_MAPS_API_KEY");

    @Test
    public void test$demo() {
        final var params = ServerParameters.builder("npx")
                .args("-y", "@amap/amap-maps-mcp-server")
                .addEnvVar("AMAP_MAPS_API_KEY", AMAP_AK)
                .build();
        final var transport = new StdioClientTransport(params);
        final var client = McpClient.sync(transport)
                .capabilities(McpSchema.ClientCapabilities.builder()
                        .sampling()
                        .build())
                .build();

        client.initialize();

        System.out.println(client.getClientInfo());

        client.close();
    }

    @Test
    public void test$demo$sync() {
        final var params = ServerParameters.builder("npx")
                .args("-y", "@amap/amap-maps-mcp-server")
                .addEnvVar("AMAP_MAPS_API_KEY", AMAP_AK)
                .build();
        final var transport = new StdioClientTransport(params);
        final var client = McpClient.sync(transport)
                .capabilities(McpSchema.ClientCapabilities.builder()
                        .sampling()
                        .build())
                .build();

        client.initialize();

        client.listTools().tools()
                .forEach(System.out::println);

        final var request = new McpSchema.CallToolRequest("maps_geo", Map.of("address", "杭州"));
        final var response = client.callTool(request);
        System.out.println(response);

        client.close();
    }

    @Test
    public void test$demo$async() {
        final var params = ServerParameters.builder("npx")
                .args("-y", "@browsermcp/mcp@latest")
                //.addEnvVar("AMAP_MAPS_API_KEY", AMAP_AK)
                .build();
        final var transport = new StdioClientTransport(params);
        final var mcpClient = McpClient.async(transport)
                .build();

        mcpClient.initialize()
                .toFuture()
                .join();

        mcpClient.listTools()
                .toFuture()
                .join()
                .tools().forEach(System.out::println);

        mcpClient.close();
    }

}
