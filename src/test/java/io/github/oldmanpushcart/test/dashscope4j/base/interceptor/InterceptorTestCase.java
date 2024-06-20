package io.github.oldmanpushcart.test.dashscope4j.base.interceptor;

import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;

public class InterceptorTestCase implements LoadingEnv {

    @Test
    public void test$interceptor$request_response_count$success() {

        final var beforeRequestCount = invokeCountInterceptor.getRequestCount();
        final var beforeResponseCount = invokeCountInterceptor.getResponseCount();

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_PLUS)
                .messages(Message.ofUser("HELLO!"))
                .build();

        client.chat(request).async().join();
        Assertions.assertEquals(invokeCountInterceptor.getRequestCount(), beforeRequestCount + 1);
        Assertions.assertEquals(invokeCountInterceptor.getResponseCount(), beforeResponseCount + 1);
        Assertions.assertEquals(invokeCountInterceptor.getSuccessCount(), beforeResponseCount + 1);
        Assertions.assertEquals(invokeCountInterceptor.getFailureCount(), beforeResponseCount);

    }

    @Test
    public void test$interceptor$request_response_count$failure() {

        final var beforeRequestCount = invokeCountInterceptor.getRequestCount();
        final var beforeResponseCount = invokeCountInterceptor.getResponseCount();

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_PLUS)
                .messages(Message.ofUser("HELLO!"))
                .timeout(Duration.ofMillis(1))
                .build();

        Assertions.assertThrows(Exception.class, ()-> client.chat(request).async().join());
        Assertions.assertEquals(invokeCountInterceptor.getRequestCount(), beforeRequestCount + 1);
        Assertions.assertEquals(invokeCountInterceptor.getResponseCount(), beforeResponseCount + 1);
        Assertions.assertEquals(invokeCountInterceptor.getSuccessCount(), beforeResponseCount);
        Assertions.assertEquals(invokeCountInterceptor.getFailureCount(), beforeResponseCount+1);

    }



}
