package io.github.oldmanpushcart.dashscope4j.agent;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

public class DebugTestCase implements LoadingEnv {


    @Test
    public void debug$mcp() {

//        final var transport = HttpClientStreamableHttpTransport
//                .builder("https://mcp.amap.com")
//                .endpoint("/mcp?key=%s".formatted(System.getenv("AMAP_MAPS_API_KEY")))
//                .openConnectionOnStartup(false)
//                .resumableStreams(false)
//                .customizeRequest(builder-> {
//                    builder.header("Mcp-Session-Id","aess_abc123");
//                })
//                .build();

        final var transport = HttpClientStreamableHttpTransport
                .builder("http://localhost:3001")
                .endpoint("/mcp")
                .build();

        final var mcpClient = McpClient.async(transport)
                .capabilities(McpSchema.ClientCapabilities.builder()
                        //.elicitation()
                        .build())
                .build();

//        final var result = mcpClient.initialize()
//                .toFuture()
//                .join();

//        System.out.println("TOOLS:");
//        mcpClient.listTools()
//                .toFuture()
//                .thenAccept(System.out::println)
//                .join();

        mcpClient.listResourceTemplates()
                .toFuture()
                .thenAccept(r -> {
                    r.resourceTemplates().forEach(System.out::println);
                })
                .join();

        System.out.println("RESOURCES:");
        mcpClient.listResources()
                .toFuture()
                .thenAccept(r -> {
                    r.resources().forEach(System.out::println);
                })
                .join();

//        System.out.println("PROMPTS:");
//        mcpClient.listPrompts()
//                .toFuture()
//                .thenAccept(System.out::println)
//                .join();

        mcpClient.closeGracefully()
                .block();

    }

}
