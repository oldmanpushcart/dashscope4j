package io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.function;

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

import java.util.HashMap;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.agent.internal.util.ResourceUtils.resourceToString;

@ChatFnName("dashscope_web_search")
@ChatFnDescription("通过关键词搜索互联网。当需要资料而没有找到合适的工具时，可以通过此工具搜索查询互联网公开资料。")
public class DashscopeWebSearchFunction implements ChatFunction<DashscopeWebSearchFunction.Parameter, DashscopeWebSearchFunction.Result> {

    private static final PromptTemplate template = new PromptTemplate(resourceToString("dashscope4j/agent/prompt/web-search-prompt.md"));

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {
        final String prompt = template.render(new HashMap<String, Object>() {{
            put("keywords", parameter.keywords());
        }});
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
    @Builder
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
