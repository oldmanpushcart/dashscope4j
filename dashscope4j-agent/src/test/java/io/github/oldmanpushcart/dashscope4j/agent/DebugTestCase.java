package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.function.SystemDateTimeFunction;
import io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.DashscopeChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.mcp.AsyncMcpChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import org.junit.jupiter.api.Test;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug() {

        final var params = ServerParameters.builder("cmd")
                .args("/c", "npx", "-y", "@amap/amap-maps-mcp-server")
                .addEnvVar("AMAP_MAPS_API_KEY", System.getenv("AMAP_MAPS_API_KEY"))
                .build();
        final var transport = new StdioClientTransport(params);

//        final var transport = HttpClientSseClientTransport.builder("https://mcp.amap.com")
//                .sseEndpoint("/sse?key=%s".formatted(System.getenv("AMAP_MAPS_API_KEY")))
//                .build();

        final var mcpClient = McpClient.async(transport)
                .build();

        mcpClient.initialize()
                .toFuture()
                .join();

        final var agent = ReActChatAgent.newBuilder()
                .client(client)
                .name("master")
                .flowBridge(true)
                .addFunction(new SystemDateTimeFunction())
                .addFunctionTool(AsyncMcpChatAgent.newBuilder()
                        .client(client)
                        .name("amap")
                        .prompt("你需要对每个请求的文本地址先确定它的经纬度坐标")
                        .flowBridge(true)
                        .mcpClient(mcpClient)
                        .build()
                        .newFunctionToolBuilder()
                        .build())
                .build();

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_PLUS)
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .addMessage(Message.ofUser("我明天想去杭州西湖赏花，请帮我规划一日游路线。我计划从杭州市\"复地黄龙和山东南门\"出发。需要根据当时的天气情况推荐我合适的出行方案。"))
                .build();

        final var response = agent.async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response.output().best().message().text());

        mcpClient.close();

    }

    @Test
    public void test$debug3() {

        final var params = ServerParameters.builder("cmd")
                .args("/c", "npx", "-y", "@browsermcp/mcp@latest")
                // .args("/c", "npx", "-y", "@modelcontextprotocol/server-filesystem", "D:\\home\\ubuntu\\workspace")
                .build();

        final var transport = new StdioClientTransport(params);
        final var mcpClient = McpClient.async(transport)
                .build();

        mcpClient.initialize()
                .toFuture()
                .join();


        final var agent = DashscopeChatAgent.newBuilder()
                .client(client)
                .flowBridge(true)
                .addFunction(new SystemDateTimeFunction())
                .addFunctionTool(AsyncMcpChatAgent.newBuilder()
                        .client(client)
                        .flowBridge(true)
                        .mcpClient(mcpClient)
                        .build()
                        .newFunctionToolBuilder()
                        .build())
                .build();

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN3_235B_A22B)
                .addMessage(Message.ofUser("新浪热点总结?"))
                .build();

        final var response = agent.async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response.output().best().message().text());

        mcpClient.close();

    }

}
