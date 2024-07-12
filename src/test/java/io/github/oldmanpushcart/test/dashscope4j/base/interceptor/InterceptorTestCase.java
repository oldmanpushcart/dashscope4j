package io.github.oldmanpushcart.test.dashscope4j.base.interceptor;

import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

public class InterceptorTestCase implements LoadingEnv {

    @Test
    public void test$interceptor$request_response_count$success() {

        final var beforeRequestCount = invokeCountInterceptor.getRequestCount();
        final var beforeResponseCount = invokeCountInterceptor.getResponseCount();
        final var beforeFailureCount = invokeCountInterceptor.getFailureCount();
        final var beforeSuccessCount = invokeCountInterceptor.getSuccessCount();

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_PLUS)
                .messages(List.of(Message.ofUser("HELLO!")))
                .build();

        client.chat(request).async().join();
        Assertions.assertEquals(beforeRequestCount + 1, invokeCountInterceptor.getRequestCount());
        Assertions.assertEquals(beforeResponseCount + 1, invokeCountInterceptor.getResponseCount());
        Assertions.assertEquals(beforeSuccessCount + 1, invokeCountInterceptor.getSuccessCount());
        Assertions.assertEquals(beforeFailureCount, invokeCountInterceptor.getFailureCount());
    }

    @Test
    public void test$interceptor$request_response_count$failure() {

        final var beforeRequestCount = invokeCountInterceptor.getRequestCount();
        final var beforeResponseCount = invokeCountInterceptor.getResponseCount();
        final var beforeFailureCount = invokeCountInterceptor.getFailureCount();
        final var beforeSuccessCount = invokeCountInterceptor.getSuccessCount();

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_PLUS)
                .messages(List.of(Message.ofUser("HELLO!")))
                .timeout(Duration.ofMillis(1))
                .build();

        Assertions.assertThrows(Exception.class, () -> client.chat(request).async().join());
        Assertions.assertEquals(invokeCountInterceptor.getRequestCount(), beforeRequestCount + 1);
        Assertions.assertEquals(invokeCountInterceptor.getResponseCount(), beforeResponseCount + 1);
        Assertions.assertEquals(invokeCountInterceptor.getSuccessCount(), beforeSuccessCount);
        Assertions.assertEquals(invokeCountInterceptor.getFailureCount(), beforeFailureCount + 1);

    }


}
