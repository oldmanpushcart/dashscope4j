package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatSearchOption;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug$text() {

        final ChatRequest request = ChatRequest.newBuilder()
                .model(new ChatModel.BaseChatModel(
                        ChatModel.Mode.TEXT,
                        "qwen3-235b-a22b",
                        ChatModel.TEXT_REMOTE,
                        new Option()
                                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                                .unmodifiable())
                )
                //.model(ChatModel.QWEN_MAX)
                .addMessage(Message.ofUser("遵化未来5天天气情况?"))
                .option(ChatOptions.ENABLE_WEB_SEARCH, true)
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .option("n", 2)
                .option("result_format", "message")
                .option(ChatOptions.SEARCH_OPTIONS, new ChatSearchOption() {{
                    forcedSearch(true);
                }})
                .option("enable_thinking", false)
                .build();

        final StringBuffer stringBuf = new StringBuffer();
        client.chat().flow(request)
                .thenAccept(responseFlow -> responseFlow
                        .map(response -> response.output().best().message().text())
                        .blockingForEach(stringBuf::append))
                .toCompletableFuture()
                .join();
        System.out.println(stringBuf);

    }

    @Test
    public void test$debug2() {
        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_VL_MAX)
                .addMessage(Message.ofUser(List.of(
                        Content.ofText("图片中戴帽子的一共几个人?"),
                        Content.ofImage(new File("./test-data/IMG_0942.JPG").toURI())
                )))
                .context(ConfigContext.class, new ConfigContext().autoUpload(true))
                .build();
        final var response = client.chat().async(request).toCompletableFuture().join();
        System.out.println(response.output().best().message().text());
    }

}
