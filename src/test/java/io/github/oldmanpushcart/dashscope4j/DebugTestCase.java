package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.files.Purpose;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug$text() {

        final FileMeta meta = client.base().files()
                .create(new File("C:\\Users\\vlinux\\Documents\\想当初知乎也是吹巨人的.docx"), Purpose.FILE_EXTRACT)
                .toCompletableFuture()
                .join();

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_LONG)
                .addMessage(Message.ofUser(List.of(
                        Content.ofText("画一张图，用途是给文档做封面插画，要求图片内容能隐喻文档主题。"),
                        Content.ofFile(meta.toURI())
                )))
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, false)
                .option(ChatOptions.ENABLE_WEB_SEARCH, true)
                .build();

        final ChatResponse response = client.chat().async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response);

        client.base().files()
                .delete(meta.identity())
                .toCompletableFuture()
                .join();

    }

    @Test
    public void test$debug$vl() {

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_VL_MAX)
                .addMessage(Message.ofUser(""))
                .build();

        final ChatResponse response = client.chat().async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response);

    }

}
