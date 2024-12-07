package io.github.oldmanpushcart.dashscope4j.api.chat;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.api.ApiResponseAssertions;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.api.ApiResponseAssertions.assertApiResponseSuccessHandler;

public class ChatTestCase extends ClientSupport {

    private static final Set<ChatModel> models = Arrays.stream(new ChatModel[]{

            // TEXT
            ChatModel.QWEN_PLUS,
            ChatModel.QWEN_TURBO,
            ChatModel.QWEN_MAX,
            ChatModel.QWEN_LONG,

            // VL
            ChatModel.QWEN_VL_MAX,
            ChatModel.QWEN_VL_PLUS,

            // AUDIO
            ChatModel.QWEN_AUDIO_CHAT,
            ChatModel.QWEN_AUDIO_TURBO,
            ChatModel.QWEN2_AUDIO_INSTRUCT

    }).collect(Collectors.toSet());

    private static ChatModel getModel(String mName) {
        return models.stream()
                .filter(m -> m.name().equals(mName))
                .findFirst()
                .orElseThrow(()-> new IllegalArgumentException("model not found: " + mName));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "qwen-plus",
            "qwen-turbo",
            "qwen-max",
            "qwen-long",
            "qwen-vl-max",
            "qwen-vl-plus"
    })
    public void test$chat$async(String mName) {
        final ChatRequest request = ChatRequest.newBuilder()
                .model(getModel(mName))
                .appendMessage(Message.ofUser("hello!"))
                .build();
        client.chat().async(request)
                .whenComplete(assertApiResponseSuccessHandler())
                .toCompletableFuture()
                .join();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "qwen-plus",
            "qwen-turbo",
            "qwen-max",
            "qwen-long",
            "qwen-vl-max",
            "qwen-vl-plus"
    })
    public void test$chat$flow(String mName) {
        final ChatRequest request = ChatRequest.newBuilder()
                .model(getModel(mName))
                .appendMessage(Message.ofUser("hello!"))
                .build();
        client.chat().flow(request)
                .thenAccept(flow -> flow.blockingForEach(ApiResponseAssertions::assertApiResponseSuccess))
                .toCompletableFuture()
                .join();
    }

}
