package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.function.SystemDateTimeFunction;
import io.github.oldmanpushcart.dashscope4j.agent.typical.mcp.AsyncMcpChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.modelcontextprotocol.client.McpClient;
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
                .model(ChatModel.QWEN_PLUS)
                .addMessage(Message.ofUser("规划一个从阿里巴巴西溪园区A9门到复地黄龙和山东南门骑车路线"))
                .build();

        final var response = agent.async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response.output().best().message().text());

        mcpClient.close();

    }

}
