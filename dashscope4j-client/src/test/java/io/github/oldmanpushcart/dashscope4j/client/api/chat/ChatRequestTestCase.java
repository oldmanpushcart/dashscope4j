package io.github.oldmanpushcart.dashscope4j.client.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.function.EchoFunction;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.content.TextContent;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

public class ChatRequestTestCase implements LoadingEnv {

    @Test
    public void test() throws InterruptedException {

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .addMessage(Message.system(TextContent.newBuilder()
                        .text("请用中文回答问题")
                        .cacheControl(Content.CacheControl.EPHEMERAL)
                        .build()))
                .addMessage(Message.user("echo: HELLO WORLD!"))
                .addFunction(new EchoFunction())
                .parameter(ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT, false)
                .build();

        final var latch = new CountDownLatch(1);
        client.chat().flow(request)
                .subscribe(new Flow.Subscriber<ChatResponse>() {

                    private volatile Flow.Subscription subscription;

                    @Override
                    public void onSubscribe(Flow.Subscription subscription) {
                        subscription.request(1);
                        this.subscription = subscription;
                    }

                    @Override
                    public void onNext(ChatResponse item) {
                        System.out.println("====" + item.output().best().message().text());
                        subscription.request(1);
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        throwable.printStackTrace();
                        latch.countDown();
                    }

                    @Override
                    public void onComplete() {
                        latch.countDown();
                    }
                });

        latch.await();

    }

}
