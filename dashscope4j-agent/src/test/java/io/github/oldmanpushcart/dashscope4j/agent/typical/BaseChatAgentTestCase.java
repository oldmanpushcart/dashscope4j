package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.ChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.agent.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.agent.function.dashscope.DashscopeUnderstandingVisualFunction;
import io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.DashscopeChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.ConfigContext;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

public class BaseChatAgentTestCase extends ClientSupport {

    public enum ChatAgentType {
        REACT,
        DASHSCOPE
    }

    private static ChatAgent newChatAgent(ChatAgentType aType, boolean flowBridge) {
        return switch (aType) {
            case REACT -> ReActChatAgent.newBuilder()
                    .client(client)
                    .flowBridge(flowBridge)
                    .addFunctionTool(ReActChatAgent.newBuilder()
                            .client(client)
                            .flowBridge(true)
                            .addFunction(new DashscopeUnderstandingVisualFunction())
                            .build()
                            .newFunctionToolBuilder()
                            .build())
                    .build();
            case DASHSCOPE -> DashscopeChatAgent.newBuilder()
                    .client(client)
                    .flowBridge(flowBridge)
                    .addFunctionTool(DashscopeChatAgent.newBuilder()
                            .client(client)
                            .flowBridge(true)
                            .addFunction(new DashscopeUnderstandingVisualFunction())
                            .build()
                            .newFunctionToolBuilder()
                            .build())
                    .build();
        };
    }

    private static Stream<Arguments> provideArgumentsForAsync() {
        final ChatModel[] models = {
                ChatModel.QWEN_TURBO,
                ChatModel.QWEN_PLUS,
                ChatModel.QWEN_MAX
        };
        return Stream.of(models)
                .flatMap(model -> Stream.of(ChatAgentType.values())
                        .map(type -> Arguments.of(model, type)));
    }

    private static Stream<Arguments> provideArgumentsForFlow() {
        final ChatModel[] models = {
                ChatModel.QWEN_TURBO,
                ChatModel.QWEN_PLUS,
                ChatModel.QWEN_MAX,
                ChatModel.QWEN3_235B_A22B
        };
        return Stream.of(models)
                .flatMap(model -> Stream.of(ChatAgentType.values())
                        .map(type -> Arguments.of(model, type)));
    }

    @ParameterizedTest
    @MethodSource("provideArgumentsForAsync")
    public void test$async(ChatModel model, ChatAgentType type) {

        final ChatAgent agent = newChatAgent(type, false);

        final ChatRequest request = ChatRequest.newBuilder()
                .model(model)
                .context(ConfigContext.class, new ConfigContext().autoUpload(true))
                .addMessage(Message.ofUser(Arrays.asList(
                        Content.ofText("图片中有几辆自行车?"),
                        Content.ofImage(new File("./test-data/image-002.jpeg").toURI())
                )))
                .build();

        final String result = agent.async(request)
                .thenApply(response -> response.output().best().message().text())
                .toCompletableFuture()
                .join();

        DashscopeAssertions.dashscopeAssertText(client, result, "有两辆自行车");
    }

    @ParameterizedTest
    @MethodSource("provideArgumentsForFlow")
    public void test$async$flowBridge(ChatModel model, ChatAgentType type) {

        final ChatAgent agent = newChatAgent(type, true);

        final ChatRequest request = ChatRequest.newBuilder()
                .model(model)
                .context(ConfigContext.class, new ConfigContext().autoUpload(true))
                .addMessage(Message.ofUser(Arrays.asList(
                        Content.ofText("有几辆自行车?"),
                        Content.ofImage(new File("./test-data/image-002.jpeg").toURI())
                )))
                .build();

        final String result = agent.async(request)
                .thenApply(response -> response.output().best().message().text())
                .toCompletableFuture()
                .join();

        DashscopeAssertions.dashscopeAssertText(client, result, "有两辆自行车");
    }


    @ParameterizedTest
    @MethodSource("provideArgumentsForFlow")
    public void test$flow(ChatModel model, ChatAgentType type) {

        final ChatAgent agent = newChatAgent(type, false);

        final ChatRequest request = ChatRequest.newBuilder()
                .model(model)
                .context(ConfigContext.class, new ConfigContext().autoUpload(true))
                .addMessage(Message.ofUser(Arrays.asList(
                        Content.ofText("有几辆自行车?"),
                        Content.ofImage(new File("./test-data/image-002.jpeg").toURI())
                )))
                .build();

        final String result = agent.directFlow(request)
                .reduce(new StringBuilder(), (stringBuf, response) -> {
                    final boolean isIncrementalOutput = ((ChatRequest) response.request()).option().has(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true);
                    if (!isIncrementalOutput) {
                        stringBuf.setLength(0);
                    }
                    stringBuf.append(response.output().best().message().text());
                    return stringBuf;
                })
                .toCompletionStage()
                .thenApply(StringBuilder::toString)
                .toCompletableFuture()
                .join();

        DashscopeAssertions.dashscopeAssertText(client, result, "有2辆自行车");
    }

}
