package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug() throws InterruptedException {

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .addMessage(Message.ofUser("你怎么看法沦功的政治主张?"))
                .build();

        final AtomicReference<Throwable> exRef = new AtomicReference<>();
        client.chat().async(request)
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    exRef.set(ex);
                    return null;
                })
                .toCompletableFuture()
                .join();

        final Throwable ex = exRef.get();
        Assertions.assertNotNull(ex);
        Assertions.assertInstanceOf(ApiException.class, ex);
        final ApiException apiEx = (ApiException) ex;
        Assertions.assertNotSame(ApiResponse.CODE_SUCCESS, apiEx.code());

//        client.chat().flow(request)
//                .thenAccept(flow -> flow
//                        .doOnError(Throwable::printStackTrace)
//                        .blockingSubscribe(r -> {
//                            System.out.println(r.output().best().message().text());
//                        }))
//                .toCompletableFuture()
//                .join();

    }


    @Test
    public void test() {
        final CompletableFuture<String> future1 = new CompletableFuture<>();
        future1.complete(null);
        future1.thenApply(v-> {
            throw new RuntimeException("test");
        })
                .exceptionally(ex-> {
                    ex.printStackTrace();
                    return null;
                })
                .toCompletableFuture()
                .join();
    }

}
