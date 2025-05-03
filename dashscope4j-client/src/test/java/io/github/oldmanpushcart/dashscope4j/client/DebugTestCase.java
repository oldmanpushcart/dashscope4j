package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatSearchOption;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import org.junit.jupiter.api.Test;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug$text() {

        final ChatRequest request = ChatRequest.newBuilder()
                //.model(ChatModel.ofText("qwen3-235b-a22b"))
                .model(ChatModel.QWEN_TURBO)
                .addMessage(Message.ofUser("遵化未来5天天气情况?"))
                .option(ChatOptions.ENABLE_WEB_SEARCH, true)
                .option(ChatOptions.SEARCH_OPTIONS, new ChatSearchOption() {{
                    forcedSearch(true);
                }})
                .option("enable_thinking", false)
                .build();

        final StringBuilder stringBuf = new StringBuilder();
        client.chat().flow(request)
                .thenAccept(responseFlow -> {
                    responseFlow
                            .map(response -> response.output().best().message().text())
                            .blockingForEach(stringBuf::append);
                })
                .toCompletableFuture()
                .join();
        System.out.println(stringBuf);

    }

}
