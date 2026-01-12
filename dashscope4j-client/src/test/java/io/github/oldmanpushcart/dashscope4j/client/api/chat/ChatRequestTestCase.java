package io.github.oldmanpushcart.dashscope4j.client.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.function.EchoFunction;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.content.TextContent;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

public class ChatRequestTestCase implements LoadingEnv {

    @Test
    public void test() throws InterruptedException {

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN3_OMNI_FLASH)
                //.model(ChatModel.QWEN_MAX)
                .addMessage(Message.system(TextContent.newBuilder()
                        .text("请用中文回答问题")
                        .cacheControl(Content.CacheControl.EPHEMERAL)
                        .build()))
                .addMessage(Message.user("echo: HELLO WORLD!"))
                //.parameter("enable_omni_output_audio_url","true")
                .addTool(new EchoFunction().toTool())
                .parameter(ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT, false)
                .build();

        final var publisher = client.chat().flow(request);
        FlowX.fromPublisher(publisher)
                .map(r -> r.output().best().message().text())
                .filter(s -> !s.isBlank())
                .blockingCollect(Collectors.toList())
                .forEach(System.out::println);

    }

    @Test
    public void test2() {

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWQ_PLUS_LATEST)
                .addMessage(Message.user("(1+2+3+4)/5=?，请告诉我最终答案"))
                //.parameter(ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT, true)
                .build();

        FlowX.fromPublisher(client.chat().flow(request))
                .reduce(ChatResponse::accumulate)
                .thenApply(response -> response.output().best().message().text())
                .thenAccept(System.out::println)
                .toCompletableFuture()
                .join();

    }

}
