package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.agent.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.agent.function.SystemDateTimeFunction;
import io.github.oldmanpushcart.dashscope4j.agent.function.dashscope.DashscopeGenImageByTextFunction;
import io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.DashscopeChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.mcp.McpChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import org.junit.jupiter.api.Test;

public class McpAgentTestCase extends ClientSupport {

    private static final String AMA_MAP_API_KEY = System.getenv("AMAP_MAPS_API_KEY");

    private final McpClientTransport transport = HttpClientSseClientTransport
            .builder("https://mcp.amap.com")
            .sseEndpoint("/sse?key=%s".formatted(AMA_MAP_API_KEY))
            .build();

    @Test
    public void test$mcp$amap$async() {

        final var mcpAgent = McpChatAgent.newBuilder()
                .client(client)
                .name("amap")
                .transport(transport)
                .build();

        final var agent = ReActChatAgent.newBuilder()
                .client(client)
                .name("master")
                .addFunction(new SystemDateTimeFunction())
                .addFunctionTool(mcpAgent.newFunctionToolBuilder()
                        .build())
                .build();

        mcpAgent.lazy()
                .toCompletableFuture()
                .join();

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_PLUS)
                .addMessage(Message.ofUser("""
                            明天想去杭州西湖赏花，请规划一日游路线。
                            计划从杭州复地黄龙和山小区出发。需要根据当时的天气情况推荐我合适的出行方案。
                            """
                ))
                .build();

        final var response = agent.async(request)
                .toCompletableFuture()
                .join();

        DashscopeAssertions.dashscopeAssertText(
                client,
                response.output().best().message().text(),
                """
                        1. 描述的是杭州西湖游玩出行方案
                        2. 至少一种出行方案
                        """
        );

    }

    @Test
    public void test$mcp$amap$weather() {

        final var mcpAgent = McpChatAgent.newBuilder()
                .client(client)
                .name("amap")
                .flowBridge(true)
                .transport(transport)
                .build();

        final var agent = ReActChatAgent.newBuilder()
                .client(client)
                .name("master")
                .flowBridge(true)
                .addFunction(new SystemDateTimeFunction())
                .addFunctionTool(DashscopeChatAgent.newBuilder()
                        .client(client)
                        .name("dashscope-tools")
                        .flowBridge(true)
                        .addFunction(DashscopeGenImageByTextFunction.newBuilder().build())
                        .build()
                        .newFunctionToolBuilder()
                        .build())
                .addFunctionTool(mcpAgent.newFunctionToolBuilder()
                        .build())
                .build();

        mcpAgent.lazy()
                .toCompletableFuture()
                .join();

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN3_235B_A22B)
                .addMessage(Message.ofUser("""
                            请播报播报杭州明天天气情况，并根据天气情况画一副水墨山水画
                            """
                ))
                .build();

        final var response = agent.async(request)
                .toCompletableFuture()
                .join();


        DashscopeAssertions.dashscopeAssertText(
                client,
                response.output().best().message().text(),
                """
                        1. 描述了天气信息
                        2. 至少包含了一张图片的URL
                        """
        );

    }

}
