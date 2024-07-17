package io.github.oldmanpushcart.test.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.base.task.Task;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.embedding.text.EmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.embedding.text.EmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageModel;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageOptions;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.util.ConsumeFlowSubscriber;
import io.github.oldmanpushcart.dashscope4j.util.ExceptionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

@Disabled
public class DebugTestCase implements LoadingEnv {

    @Test
    public void test$debug$text() {

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_VL_MAX)
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .messages(List.of(
                        Message.ofUser(List.of(
                                Content.ofImage(new File("./document/image/image-002.jpeg").toURI()),
                                Content.ofText("图片中一共多少辆自行车?")
                        ))
                ))
                .build();

//        {
//            final var response = client.chat(request).async()
//                    .join();
//            System.out.println(response.output().best().message().text());
//        }

        {
            client.chat(request).flow()
                    .thenCompose(publisher -> ConsumeFlowSubscriber.consumeCompose(publisher, r -> {
                        System.out.println(r);
                        DashScopeAssertions.assertChatResponse(r);
                    }))
                    .join();
        }


    }

    @Test
    public void test$debug$image$gen() {
        final var request = GenImageRequest.newBuilder()
                .model(GenImageModel.WANX_V1)
                .option(GenImageOptions.NUMBER, 1)
                .prompt("画一只猫")
                .build();
        final var response = client.image().generation(request)
                .task(Task.WaitStrategies.perpetual(Duration.ofMillis(1000L)))
                .join();
        System.out.println(response);
    }

    @Test
    public void test$debug$embedding$request() {

        final var request = EmbeddingRequest.newBuilder()
                .model(EmbeddingModel.TEXT_EMBEDDING_V2)
                .documents(List.of("我爱北京天安门", "天安门上太阳升"))
                .build();

        final var response = client.embedding().text(request)
                .async()
                .join();

        System.out.println(response);

    }

    @Test
    public void test$debug$ratelimit() throws Exception {
        final var total = 201;
        final var successRef = new AtomicInteger();
        final var failureRef = new AtomicInteger();
        final var launch = new CountDownLatch(total);
        for (int i = 0; i < total; i++) {
            final var request = ChatRequest.newBuilder()
                    .model(ChatModel.QWEN_PLUS)
                    .messages(List.of(
                            Message.ofUser("我有100块钱存银行，利率3个点，请告诉我%s年后，我连本带利有多少钱?".formatted(i+1))
                    ))
                    .build();
            client.chat(request).async().whenComplete((r, ex) -> {
                if (null != ex) {
                    failureRef.incrementAndGet();
                } else {
                    successRef.incrementAndGet();
                }
                launch.countDown();
            });
        }

        launch.await();
        System.out.println("success: " + successRef.get());
        System.out.println("failure: " + failureRef.get());
        Assertions.assertEquals(total, successRef.get() + failureRef.get());
    }

    @Test
    public void test$debug() throws InterruptedException {

        final var latch = new CountDownLatch(1);
        CompletableFuture.completedFuture("hello")
                .thenApplyAsync(it -> it + " world")
                .thenApply(it -> it + "!")
                .thenAccept(s-> {
                    throw new IllegalArgumentException("test error!");
                })
                .whenComplete((r,ex)-> {

                    if(null != ex) {
                        final var cause = ExceptionUtils.causeBy(ex, UnsupportedOperationException.class);
                        System.out.println(null == cause);
                    }
                    latch.countDown();
                });

        latch.await();

    }

}
