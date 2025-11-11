package io.github.oldmanpushcart.dashscope4j.client;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.*;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.function.EchoFunction;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.ToolCallMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.JacksonJsonUtils;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

public class DebugTestCase implements LoadingEnv {

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    public void debug() throws InterruptedException {

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_PLUS)
                .addMessage(Message.ofUser("echo:你好呀！"))
                .addFunction(new EchoFunction())
                .parameter(ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT, true)
                .build();

        final var chatOp = ChatOp.newBuilder()
                .ak(AK)
                .http(http)
                .build();

        final var latch = new CountDownLatch(1);
        chatOp.flow(request).subscribe(new Flow.Subscriber<>() {

            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(1);
            }

            @Override
            public void onNext(ChatResponse item) {
                System.out.println("===="+item.output().best().message().text());
                subscription.request(1);
            }

            @Override
            public void onError(Throwable ex) {
                ex.printStackTrace();
                latch.countDown();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }

        });

        latch.await();

    }

    @Test
    public void debug2() {

        final var calls = new ArrayList<Tool.Call>();
        Tool.Call call = new FunctionTool.Call(0, "echo", new FunctionTool.Call.Stub("echo", "{\"text\":\"HELLO!\"}"));
        calls.add(call);

        final var message = new ToolCallMessage("echo: HELLO!", calls);
        final var json = JacksonJsonUtils.toJson(ChatViews.Text.class, message);
        System.out.println(json);

    }

}
