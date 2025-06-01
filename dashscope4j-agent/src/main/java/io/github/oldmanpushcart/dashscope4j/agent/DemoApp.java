package io.github.oldmanpushcart.dashscope4j.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oldmanpushcart.dashscope4j.agent.function.SystemDateTimeFunction;
import io.github.oldmanpushcart.dashscope4j.agent.function.dashscope.DashscopeGenImageByTextFunction;
import io.github.oldmanpushcart.dashscope4j.agent.typical.mcp.McpChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.mcp.McpClientKeeper;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.util.JsonUtils;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;

import java.util.List;

@Slf4j
public class DemoApp {

    public static void main(String... args) throws Exception {

        final var transportProvider = new HttpServletSseServerTransportProvider(new ObjectMapper(), "/mcp/message");

        final var mcpServer = McpServer.sync(transportProvider)
                .serverInfo("test", "0.0.1")
                .capabilities(McpSchema.ServerCapabilities
                        .builder()
                        .tools(true)
                        .build())
                .build();

        final var dashscope = DashscopeClient.newBuilder()
                .ak(System.getenv("DASHSCOPE_AK"))
                .build();

        final var keeper = new McpClientKeeper();
        final var agent = ReActChatAgent.newBuilder()
                .client(dashscope)
                .addFunction(new SystemDateTimeFunction())
                .addFunction(DashscopeGenImageByTextFunction.newBuilder().build())
                .addFunctionTool(McpChatAgent.newBuilder()
                        .client(dashscope)
                        .mcpClientRegistration(keeper.register("amap", () -> McpClient
                                .async(HttpClientSseClientTransport
                                        .builder("https://mcp.amap.com")
                                        .sseEndpoint("/sse?key=%s".formatted(System.getenv("AMAP_MAPS_API_KEY")))
                                        .build())
                                .build()))
                        .build()
                        .newFunctionToolBuilder()
                        .build())
                .build();

        final var functionTool = agent
                .newFunctionToolBuilder()
                .build();

        mcpServer.addTool(new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool(functionTool.meta().name(), functionTool.meta().description(), functionTool.meta().parameterSchema().toString()),
                (exchange, params) -> {
                    final var arguments = JsonUtils.toJson(params);
                    final var request = ChatRequest.newBuilder()
                            .model(ChatModel.QWEN_PLUS)
                            .addMessage(Message.ofUser(arguments))
                            .build();

                    final var response = agent.async(request)
                            .toCompletableFuture()
                            .join();
                    return McpSchema.CallToolResult
                            .builder()
                            .textContent(List.of(response.output().best().message().text()))
                            .build();
                }
        ));


        Server server = new Server(9999);

        // 创建 Servlet 上下文处理器
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        // 注册你的 Servlet 并设置访问路径
        context.addServlet(transportProvider, "/sse");
        context.addServlet(transportProvider, "/mcp/message");

        // 启动服务器
        server.start();
        server.join();


    }

}
