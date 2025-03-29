package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.chat.*;
import io.github.oldmanpushcart.dashscope4j.api.chat.function.QueryScoreFunction;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import org.junit.jupiter.api.Test;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug$text() {

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_MAX)
                .addFunction(new QueryScoreFunction())
                .addMessage(Message.ofUser("杭州明天天气如何？"))
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .option(ChatOptions.ENABLE_PARALLEL_TOOL_CALLS, true)
                .option(ChatOptions.ENABLE_WEB_SEARCH, true)
                .option(ChatOptions.SEARCH_OPTIONS, new ChatSearchOption()
                        .forcedSearch(true)
                        .enableSource(true)
                        .enableCitation(true)
                        .searchStrategy(ChatSearchOption.SearchStrategy.STANDARD)
                )
                .build();
        client.chat().directFlow(request)
                .doOnNext(response-> System.out.println(response.output().best().message().text()))
                .blockingSubscribe();

    }

}
