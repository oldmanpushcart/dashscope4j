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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Duration;
import java.util.List;
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
    public void test$debug() {
        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_PLUS)
                .messages(List.of(
                        Message.ofUser("""
                                常见问题
                                通义千问、灵积、DashScope、百炼是什么关系？
                                通义千问是阿里云研发的大语言模型；灵积是阿里云推出的模型服务平台，提供了包括通义千问在内的多种模型的服务接口，DashScope是灵积的英文名，两者指的是同一平台；百炼是阿里云推出的一站式大模型应用开发平台。
                                我如果想调用通义千问模型，是要通过DashScope还是百炼平台？
                                对于需要模型调用的开发者而言，通过DashScope与百炼平台调用通义千问模型都是通过dashscope SDK或OpenAI兼容或HTTP方式实现。两个平台都可以获取到API-KEY，且是同步的。因此您只需准备好计算环境，并在两个平台任选其一创建API-KEY，即可发起通义千问模型的调用。
                                我想通过灵积调用通义千问开源模型，但是文档中没有相应介绍，我该看哪篇文档？
                                qwen开源模型的使用方法请参考大语言模型文档。
                                我可以通过OpenAI兼容方式调用通义千问的多模态模型吗？
                                可以，详情请您参考VL模型流式调用示例（输入图片url）。
                                
                                请帮我将上边的段落总结，要求生成的token在100-200之间
                                """
                        )
                ))
                .build();
        final var response = client.chat(request).async().join();
        System.out.printf(
                "usage=%s;text=%s%n", response.usage(),
                response.output().best().message().text()
        );
    }

}
