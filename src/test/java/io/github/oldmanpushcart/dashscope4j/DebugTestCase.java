package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.ApiAssertions;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug() throws InterruptedException, ExecutionException {
        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .addMessage(Message.ofUser("你好!"))
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .build();
        client.chat().flow(request)
                .thenAccept(flow -> flow
                        .doOnNext(ApiAssertions::assertApiResponseSuccessful)
                        .doOnError(Assertions::fail)
                        .blockingSubscribe()
                )
                .toCompletableFuture()
                .join();

    }

}
