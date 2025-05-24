package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.function.SystemDateTimeFunction;
import io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.DashscopeChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.mcp.AsyncMcpChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import org.junit.jupiter.api.Test;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug() {

        final var params = ServerParameters.builder("npx")
                .args("-y", "@amap/amap-maps-mcp-server")
                .addEnvVar("AMAP_MAPS_API_KEY", System.getenv("AMAP_MAPS_API_KEY"))
                .build();
        final var transport = new StdioClientTransport(params);
        final var mcpClient = McpClient.async(transport)
                .build();

        mcpClient.initialize()
                .toFuture()
                .join();

        final var agent = ReActChatAgent.newBuilder()
                .client(client)
                .flowBridge(true)
                .addFunction(new SystemDateTimeFunction())
                .addFunctionTool(AsyncMcpChatAgent.newBuilder()
                        .client(client)
                        .flowBridge(true)
                        .prompt("""
                                你需要对每一个地址都仔细核对它的经纬度
                                """)
                        .mcpClient(mcpClient)
                        .build()
                        .newFunctionToolBuilder()
                        .build())
                .build();

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN3_235B_A22B)
                .addMessage(Message.ofUser("在阿里巴巴西溪园区A9门到复地黄龙和山东南门之间找一个咖啡馆"))
                .build();

        final var response = agent.async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response.output().best().message().text());

        mcpClient.close();

    }

    @Test
    public void test$debug3() {

        final var params = ServerParameters.builder("npx")
                // .args("-y", "@browsermcp/mcp@latest")
                .args("-y", "@playwright/mcp@latest")
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
                .addMessage(Message.ofUser("从京东网上找苹果16手机的最新价格"))
                .build();

        final var response = agent.async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response.output().best().message().text());

        mcpClient.close();

    }

}
