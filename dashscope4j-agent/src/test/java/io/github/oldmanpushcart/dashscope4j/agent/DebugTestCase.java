package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.typical.mcp.RecoverableMcpClientTransport;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug() {

        final McpClientTransport transport = RecoverableMcpClientTransport.newBuilder()
                .transportFactory(mapper-> {
                    return HttpClientSseClientTransport
                            .builder("https://mcp.amap.com")
                            .objectMapper(mapper)
                            .sseEndpoint("/sse?key=%s".formatted(System.getenv("AMAP_MAPS_API_KEY")))
                            .build();
                })
                .pingEnabled(true)
                .build();


        final McpAsyncClient client = McpClient.async(transport)
                .capabilities(McpSchema.ClientCapabilities.builder().build())
                .build();

        try {
            client.initialize().toFuture().join();
            client.listTools()
                    .map(McpSchema.ListToolsResult::tools)
                    .flatMap(tools -> {
                        for (McpSchema.Tool tool : tools) {
                            System.out.println(tool.name());
                        }
                        return Mono.empty();
                    })
                    .toFuture().join();
        } finally {
            client.close();
        }


    }

}
