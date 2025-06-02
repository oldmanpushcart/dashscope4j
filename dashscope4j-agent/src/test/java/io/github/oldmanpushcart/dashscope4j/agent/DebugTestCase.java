package io.github.oldmanpushcart.dashscope4j.agent;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug() {

        final var transport = HttpClientSseClientTransport
                .builder("https://mcp.amap.com")
                .sseEndpoint("/sse?key=%s".formatted(System.getenv("AMAP_MAPS_API_KEY")))
                .build();

        final var client = McpClient.async(transport)
                .toolsChangeConsumer(new Function<List<McpSchema.Tool>, Mono<Void>>() {
                    @Override
                    public Mono<Void> apply(List<McpSchema.Tool> tools) {
                        System.out.println(tools);
                        return Mono.empty();
                    }
                })
                .build();

        client.initialize()
                .toFuture()
                .thenAccept(r->{
                    System.out.println(r);
                })
                .join();

        client.close();

    }

}
