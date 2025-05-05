package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.ChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.agent.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.agent.function.dashscope.DashscopeUnderstandingVisualFunction;
import io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.DashscopeChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.Option;
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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BaseChatAgentTestCase extends ClientSupport {

    private static final Set<ChatModel> models = Arrays.stream(new ChatModel[]{
            ChatModel.QWEN_PLUS,
            ChatModel.QWEN_TURBO,
            ChatModel.QWEN_MAX,
            ChatModel.BaseChatModel.ofText("qwen3-235b-a22b", new Option()
                    .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                    .option("enable_thinking", false)
                    .unmodifiable())
    }).collect(Collectors.toSet());

    private static ChatModel getModel(String mName) {
        return models.stream()
                .filter(m -> m.name().equals(mName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("model not found: " + mName));
    }

    private static ChatAgent newChatAgent(String aName, boolean flowBridgeEnabled) {
        if ("react".equals(aName)) {
            return ReActChatAgent.newBuilder()
                    .client(client)
                    .enableFlowBridge(flowBridgeEnabled)
                    .addFunction(new DashscopeUnderstandingVisualFunction()
                            .autoUpload(true))
                    .build();
        }
        if ("dashscope".equals(aName)) {
            return DashscopeChatAgent.newBuilder()
                    .client(client)
                    .enableFlowBridge(flowBridgeEnabled)
                    .addFunction(new DashscopeUnderstandingVisualFunction()
                            .autoUpload(true))
                    .build();
        }
        throw new IllegalArgumentException("agent not found: " + aName);
    }

    private static Stream<Arguments> providerForAsync() {
        final String[] mNames = {"qwen-plus", "qwen-max"};
        final String[] aNames = {"react", "dashscope"};
        return Stream.of(mNames)
                .flatMap(mName -> Stream.of(aNames)
                        .map(aName -> Arguments.of(mName, aName)));
    }

    private static Stream<Arguments> providerForFlow() {
        final String[] mNames = {"qwen-plus", "qwen-max", "qwen3-235b-a22b"};
        final String[] aNames = {"react", "dashscope"};
        return Stream.of(mNames)
                .flatMap(mName -> Stream.of(aNames)
                        .map(aName -> Arguments.of(mName, aName)));
    }

    @ParameterizedTest
    @MethodSource("providerForAsync")
    public void test$async(String mName, String aName) {

        final ChatAgent agent = newChatAgent(aName, false);

        final ChatRequest request = ChatRequest.newBuilder()
                .model(getModel(mName))
                .addMessage(Message.ofUser(Arrays.asList(
                        Content.ofText("有几辆自行车?"),
                        Content.ofImage(new File("./test-data/image-002.jpeg").toURI())
                )))
                .build();

        final String result = agent.async(request)
                .thenApply(response -> response.output().best().message().text())
                .toCompletableFuture()
                .join();

        DashscopeAssertions.assertByDashscope(client, "有两辆自行车", result);
    }

    @ParameterizedTest
    @MethodSource("providerForFlow")
    public void test$async$flowBridge(String mName, String aName) {

        final ChatAgent agent = newChatAgent(aName, true);

        final ChatRequest request = ChatRequest.newBuilder()
                .model(getModel(mName))
                .addMessage(Message.ofUser(Arrays.asList(
                        Content.ofText("有几辆自行车?"),
                        Content.ofImage(new File("./test-data/image-002.jpeg").toURI())
                )))
                .build();

        final String result = agent.async(request)
                .thenApply(response -> response.output().best().message().text())
                .toCompletableFuture()
                .join();

        DashscopeAssertions.assertByDashscope(client, "有两辆自行车", result);
    }


    @ParameterizedTest
    @MethodSource("providerForFlow")
    public void test$flow(String mName, String aName) {

        final ChatAgent agent = newChatAgent(aName, false);

        final ChatRequest request = ChatRequest.newBuilder()
                .model(getModel(mName))
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

        DashscopeAssertions.assertByDashscope(client, "有两辆自行车", result);
    }

}
