package io.github.oldmanpushcart.dashscope4j.agent.function.dashscope;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.prompt.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatSearchOption;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;
import lombok.Builder;

import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

@ChatFnName("dashscope_web_search")
@ChatFnDescription("通过关键词搜索互联网。当需要资料而没有找到合适的工具时，可以通过此工具搜索查询互联网公开资料。")
@Builder(builderMethodName = "newBuilder")
public class DashscopeWebSearchFunction
        implements ChatFunction<DashscopeWebSearchFunction.Parameter, DashscopeWebSearchFunction.Result> {

    @Builder.Default
    private UnaryOperator<ChatRequest> requestTransformer = t -> t;

    @Override
    public CompletionStage<Result> call(Tool.Caller caller, Parameter parameter) {
        final var request = ChatRequest.newBuilder()
                .copyContextFrom(caller.request())
                .model(ChatModel.QWEN_PLUS)
                .option(ChatOptions.ENABLE_WEB_SEARCH, true)
                .option(ChatOptions.SEARCH_OPTIONS, new ChatSearchOption() {{
                    forcedSearch(true);
                }})
                .addMessage(Message.ofUser(PromptTemplate.newBuilder()
                        .template("""
                                ## 根据关键词搜索
                                请使用以下关键词执行网络搜索，并按照指示的方式简洁回答。
                                
                                ## 搜索关键词
                                ${keywords}
                                """)
                        .variable("keywords", parameter.keywords())
                        .build()
                        .render()))
                .build();

        final var newRequest = requestTransformer.apply(request);

        return caller.client().chat().async(newRequest)
                .thenApply(response -> response.output().best().message().text())
                .thenApply(Result::new);
    }


    public record Parameter(
            @JsonProperty(required = true)
            @JsonPropertyDescription("搜索关键词")
            String keywords
    ) {

    }

    public record Result(
            @JsonPropertyDescription("搜索结果")
            @JsonProperty
            String output
    ) {

    }

}
