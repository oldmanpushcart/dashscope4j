package io.github.oldmanpushcart.dashscope4j.api.chat;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.api.chat.function.QueryScoreFunction;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import org.junit.jupiter.api.Test;

import static io.github.oldmanpushcart.dashscope4j.api.chat.ChatOptions.ENABLE_PARALLEL_TOOL_CALLS;

public class ChatToolCallTestCase extends ClientSupport {

    @Test
    public void test$chat$tool$function$async() {

        final ChatRequest request = ChatRequest.newBuilder()
                .option(ENABLE_PARALLEL_TOOL_CALLS, true)
                .model(ChatModel.QWEN_TURBO)
                .addFunction(new QueryScoreFunction())
                .addMessage(Message.ofUser("查询数学和语文的成绩"))
                .build();

        final ChatResponse response = client.chat().async(request)
                .toCompletableFuture()
                .join();

        DashscopeAssertions.assertByDashscope(client, "数学的成绩是88.0分，语文的成绩是100.0分", response.output().best().message().text());

    }

    @Test
    public void test$chat$tool$function$flow() {

        final ChatRequest request = ChatRequest.newBuilder()
                .option(ENABLE_PARALLEL_TOOL_CALLS, true)
                .model(ChatModel.QWEN_TURBO)
                .addFunction(new QueryScoreFunction())
                .addMessage(Message.ofUser("查询数学和语文的成绩"))
                .build();

        final StringBuilder stringBuf = new StringBuilder();
        client.chat().directFlow(request)
                .map(response-> response.output().best().message().text())
                .doOnNext(stringBuf::append)
                .blockingSubscribe();

        DashscopeAssertions.assertByDashscope(client, "数学的成绩是88.0分，语文的成绩是100.0分", stringBuf.toString());


    }

}
