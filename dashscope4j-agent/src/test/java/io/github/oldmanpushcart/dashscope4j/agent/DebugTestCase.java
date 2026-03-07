package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.tool.DashscopeTools;
import io.github.oldmanpushcart.dashscope4j.agent.tool.SystemTools;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.ArrayList;

public class DebugTestCase implements LoadingEnv {

    @Test
    public void debug$async() {

        final var tools = new ArrayList<Tool>();
        tools.addAll(SystemTools.tools());
        tools.addAll(DashscopeTools.tools());

        final var agent = ReActChatAgent.newBuilder()
                .client(client)
                .name("debug-agent")
                .description("just a test")
                .introduction("你是一个工具助手")
                .model(ChatModel.QWEN_FLASH)
                .tools(tools)
                .build();

        final var image = new File("./test-data/image/IMG_0942.JPG").toURI();
        final var outbound = agent
                .async(Message.user("""
                        分析桌面的images文件夹的所有图片，并总结内容，表格呈现
                        """
                ))
                .toCompletableFuture()
                .join();

        System.out.println(outbound.text());

    }

    @Test
    public void debug$flow() {

        final var tools = new ArrayList<Tool>();
        tools.addAll(SystemTools.tools());
        tools.addAll(DashscopeTools.tools());

        final var agent = ReActChatAgent.newBuilder()
                .client(client)
                .name("debug-agent")
                .description("just a test")
                .introduction("你是一个工具助手")
                .model(ChatModel.QWEN_FLASH)
                .tools(tools)
                .build();

        final var image = new File("./test-data/image/IMG_0942.JPG").toURI();
        final var responseFlow = agent
                .flow(Message.user("""
                        分析桌面的images文件夹的所有图片，并总结内容，表格呈现
                        """
                ));

        final var outbound = Flux.from(responseFlow)
                .reduce(AssistantMessage::accumulate)
                .toFuture()
                .join();

        Assertions.assertNotNull(outbound);
        System.out.println(outbound.text());

    }

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
                .openConnectionOnStartup(true)
                .build();

        final var mcpClient = McpClient.async(transport)
                .capabilities(McpSchema.ClientCapabilities.builder()
                        //.elicitation()
                        .build())
                .build();

//        final var result = mcpClient.initialize()
//                .toFuture()
//                .join();

        System.out.println("TOOLS:");
        mcpClient.listTools()
                .toFuture()
                .thenAccept(System.out::println)
                .join();

        System.out.println("RESOURCES:");
        mcpClient.listResources()
                .toFuture()
                .thenAccept(System.out::println)
                .join();

        System.out.println("PROMPTS:");
        mcpClient.listPrompts()
                .toFuture()
                .thenAccept(System.out::println)
                .join();

        mcpClient.closeGracefully()
                .block();

    }

}
