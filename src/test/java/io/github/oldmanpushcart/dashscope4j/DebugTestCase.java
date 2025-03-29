package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.ApiAssertions;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.function.QueryScoreFunction;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.github.oldmanpushcart.dashscope4j.api.ApiAssertions.assertApiResponseSuccessful;
import static io.github.oldmanpushcart.dashscope4j.api.chat.ChatOptions.ENABLE_PARALLEL_TOOL_CALLS;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug$text() {

        final ChatRequest request = ChatRequest.newBuilder()
                .option(ENABLE_PARALLEL_TOOL_CALLS, true)
                .model(ChatModel.QWEN_TURBO)
                .addFunction(new QueryScoreFunction())
                .addMessage(Message.ofUser("查询数学和语文的成绩"))
                .build();
        client.chat().flow(request)
                .thenAccept(flow -> flow
                        .doOnNext(ApiAssertions::assertApiResponseSuccessful)
                        .doOnError(Assertions::fail)
                        .reduce((a, b) -> b)
                        .blockingSubscribe(response -> {
                            assertApiResponseSuccessful(response);
                            final String text = response.output().best().message().text();
                            System.out.println(text);
                        }))
                .toCompletableFuture()
                .join();

    }

}
