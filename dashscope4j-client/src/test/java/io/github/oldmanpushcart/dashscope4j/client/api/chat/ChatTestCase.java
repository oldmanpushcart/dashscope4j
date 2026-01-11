package io.github.oldmanpushcart.dashscope4j.client.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiAssertions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

public class ChatTestCase implements LoadingEnv {

    static Stream<ChatModel> provideModelsForText() {
        return Stream.of(
                ChatModel.QWEN_TURBO,
                ChatModel.QWEN_PLUS,
                ChatModel.QWEN_MAX,
                ChatModel.QWEN_LONG,
                ChatModel.QWEN_VL_PLUS,
                ChatModel.QWEN_VL_MAX,
                ChatModel.QWQ_PLUS,
                ChatModel.QWQ_PLUS_LATEST,
                ChatModel.QVQ_MAX,
                ChatModel.QWEN3_OMNI_FLASH
        );
    }

    @ParameterizedTest
    @MethodSource("provideModelsForText")
    public void test$text$chat$async(ChatModel model) {
        final var request = ChatRequest.newBuilder()
                .model(model)
                .addMessage(Message.user("(1+2+3+4)/5=?"))
                .build();
        final var response = client.chat().async(request)
                .toCompletableFuture()
                .join();
        ApiAssertions.assertApiResponseSuccessful(response);
        DashscopeAssertions.dashscopeAssertText(client, response.output().best().message().text(), "答案是2");
    }

    @ParameterizedTest
    @MethodSource("provideModelsForText")
    public void test$text$chat$flow(ChatModel model) {
        final var request = ChatRequest.newBuilder()
                .model(model)
                .addMessage(Message.user("(1+2+3+4)/5=?"))
                .build();
        final var response = FlowX.fromPublisher(client.chat().flow(request))
                .doOnNext(ApiAssertions::assertApiResponseSuccessful)
                .reduce(ChatResponse::accumulate)
                .toCompletableFuture()
                .join();
        ApiAssertions.assertApiResponseSuccessful(response);
        DashscopeAssertions.dashscopeAssertText(client, response.output().best().message().text(), "答案是2");
    }

}
