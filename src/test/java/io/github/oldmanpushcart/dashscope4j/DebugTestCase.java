package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.files.Purpose;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug() throws InterruptedException, ExecutionException {
        final FileMeta meta = client.base().files().create(new File("./test-data/P020210313315693279320.pdf"), Purpose.FILE_EXTRACT)
                .toCompletableFuture()
                .join();
        System.out.println(meta);
    }

    @Test
    public void test$debug1() throws IOException, InterruptedException {
        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .addMessage(Message.ofUser("你好呀!"))
                .build();

        final ChatResponse response = client.chat().async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response.output().best().message().text());
    }

}
