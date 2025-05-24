package io.github.oldmanpushcart.dashscope4j.client.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.ConfigContext;
import io.github.oldmanpushcart.dashscope4j.client.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;

import static io.github.oldmanpushcart.dashscope4j.client.api.ApiAssertions.assertApiResponseSuccessful;

public class ChatQvQTestCase extends ClientSupport {

    @Test
    public void test$chat$vision$local$image() {

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QVQ_MAX)
                .context(ConfigContext.class, new ConfigContext().autoUpload(true))
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .addMessage(Message.ofUser(Arrays.asList(
                        Content.ofImage(new File("./test-data/IMG_0942.JPG").toURI()),
                        Content.ofText("图片中一共多少个男孩?")
                )))
                .build();

        final StringBuilder stringBuf = new StringBuilder();
        client.chat().flow(request)
                .thenCompose(responseFlow ->
                        responseFlow
                                .doOnNext(response -> {
                                    assertApiResponseSuccessful(response);
                                    stringBuf.append(response.output().best().message().text());
                                })
                                .toList()
                                .toCompletionStage())
                .toCompletableFuture()
                .join();

        DashscopeAssertions.dashscopeAssertText(client, "图片中一共有5个男孩", stringBuf.toString());

    }

}
