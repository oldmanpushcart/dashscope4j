package io.github.oldmanpushcart.dashscope4j.agent.function.dashscope;

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
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.concurrent.CompletionStage;

@ChatFnName("dashscope_web_search")
@ChatFnDescription("通过关键词搜索互联网。当需要资料而没有找到合适的工具时，可以通过此工具搜索查询互联网公开资料。")
public class DashscopeWebSearchFunction
        implements ChatFunction<DashscopeWebSearchFunction.Parameter, DashscopeWebSearchFunction.Result> {

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {
        final String prompt = PromptTemplate.newBuilder()
                .template("## 根据关键词搜索\n" +
                          "请使用以下关键词执行网络搜索，并按照指示的方式简洁回答。\n" +
                          "\n" +
                          "## 搜索关键词\n" +
                          "${keywords}")
                .variable("keywords", parameter.keywords())
                .build()
                .render();
        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_PLUS)
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
    @Jacksonized
    @Builder(builderMethodName = "newBuilder")
    public static class Parameter {

        @JsonProperty(required = true)
        @JsonPropertyDescription("搜索关键词")
        String keywords;

    }

    @Value
    @Accessors(fluent = true)
    public static class Result {

        @JsonPropertyDescription("搜索结果")
        @JsonProperty
        String output;

    }

}
