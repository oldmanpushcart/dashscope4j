package io.github.oldmanpushcart.dashscope4j.api.chat;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.api.chat.function.EchoFunction;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.api.chat.plugin.ChatPlugin;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.api.ApiAssertions.thenAcceptAssertByFlowForApiResponseSuccessful;
import static io.github.oldmanpushcart.dashscope4j.api.ApiAssertions.whenCompleteAssertByAsyncForApiResponseSuccessful;
import static io.github.oldmanpushcart.dashscope4j.api.chat.ChatAssertions.thenApplyAssertByFlowForChatResponseMessageTextContains;
import static io.github.oldmanpushcart.dashscope4j.api.chat.ChatAssertions.whenCompleteAssertByAsyncForChatResponseMessageTextContains;

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
                .orElseThrow(() -> new IllegalArgumentException("model not found: " + mName));
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
                .addMessage(Message.ofUser("hello!"))
                .build();
        client.chat().async(request)
                .whenComplete(whenCompleteAssertByAsyncForApiResponseSuccessful())
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
                .addMessage(Message.ofUser("hello!"))
                .build();
        client.chat().flow(request)
                .thenAccept(thenAcceptAssertByFlowForApiResponseSuccessful())
                .toCompletableFuture()
                .join();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "qwen-plus",
            "qwen-turbo",
            "qwen-max"
    })
    public void test$chat$async$plugin$calculator(String mName) {
        final ChatRequest request = ChatRequest.newBuilder()
                .model(getModel(mName))
                .addPlugin(ChatPlugin.CALCULATOR)
                .addMessage(Message.ofUser("1+2*3-4/5=?"))
                .build();
        client.chat().async(request)
                .whenComplete(whenCompleteAssertByAsyncForApiResponseSuccessful())
                .toCompletableFuture()
                .join();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "qwen-plus",
            "qwen-turbo",
            "qwen-max"
    })
    public void test$chat$async$plugin$pdf_extracter(String mName) {
        final ChatRequest request = ChatRequest.newBuilder()
                .model(getModel(mName))
                .addPlugin(ChatPlugin.PDF_EXTRACTER)
                .addMessage(Message.ofUser(Arrays.asList(
                        Content.ofText("请总结这篇文档"),
                        Content.ofFile(URI.create("https://ompc.oss-cn-hangzhou.aliyuncs.com/share/P020210313315693279320.pdf"))
                )))
                .build();
        client.chat().async(request)
                .whenComplete(whenCompleteAssertByAsyncForApiResponseSuccessful())
                .toCompletableFuture()
                .join();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "qwen-plus",
            "qwen-turbo",
            "qwen-max"
    })
    public void test$chat$async$tool$function$echo(String mName) {
        final ChatRequest request = ChatRequest.newBuilder()
                .model(getModel(mName))
                .addFunction(new EchoFunction())
                .addMessage(Message.ofUser("echo: HELLO!"))
                .build();
        client.chat().async(request)
                .whenComplete(whenCompleteAssertByAsyncForApiResponseSuccessful())
                .whenComplete(whenCompleteAssertByAsyncForChatResponseMessageTextContains("HELLO!"))
                .toCompletableFuture()
                .join();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "qwen-plus",
            "qwen-turbo",
            "qwen-max"
    })
    public void test$chat$flow$tool$function$echo(String mName) {
        final ChatRequest request = ChatRequest.newBuilder()
                .model(getModel(mName))
                .addFunction(new EchoFunction())
                .addMessage(Message.ofUser("echo: HELLO!"))
                .build();
        client.chat().flow(request)
                .thenApply(thenApplyAssertByFlowForChatResponseMessageTextContains(false, "HELLO!"))
                .thenAccept(thenAcceptAssertByFlowForApiResponseSuccessful())
                .toCompletableFuture()
                .join();
    }

}
