package io.github.oldmanpushcart.dashscope4j.agent.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.prompt.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatSearchOption;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.concurrent.CompletionStage;

@ChatFnName("dashscope_web_search")
@ChatFnDescription("通过关键词搜索互联网")
public class DashscopeWebSearchFunction implements ChatFunction<DashscopeWebSearchFunction.Parameter, DashscopeWebSearchFunction.Result> {

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {
        final String prompt = new PromptTemplate(
                "根据以下关键词进行搜索，并提供相关的结果。\n" +
                "请以简洁的方式列出与上述关键词最相关的信息、定义、用途或应用场景。确保信息准确、来源可靠。\n" +
                "关键词：${keywords}")
                .parameter("keywords", parameter.keywords())
                .render();
        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .option(ChatOptions.ENABLE_WEB_SEARCH, true)
                .option(ChatOptions.SEARCH_OPTIONS, new ChatSearchOption() {{
                    forcedSearch(true);
                }})
                .addMessage(Message.ofUser(prompt))
                .build();
        return caller.client().chat().async(request)
                .thenApply(response -> response.output().best().message().text())
                .thenApply(Result::new);
    }

    @Value
    @Accessors(fluent = true)
    public static class Parameter {

        @JsonProperty(required = true)
        @JsonPropertyDescription("搜索关键词")
        String keywords;

    }

    @Value
    public static class Result {

        @JsonPropertyDescription("搜索结果")
        @JsonProperty
        String output;

    }

}
