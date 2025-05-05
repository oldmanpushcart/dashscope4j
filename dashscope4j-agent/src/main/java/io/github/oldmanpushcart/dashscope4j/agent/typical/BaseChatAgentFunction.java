package io.github.oldmanpushcart.dashscope4j.agent.typical;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.concurrent.CompletionStage;

/**
 * 基础智能体函数
 */
class BaseChatAgentFunction
        implements ChatFunction<BaseChatAgentFunction.Parameter, BaseChatAgentFunction.Result> {

    private final BaseChatAgent agent;

    public BaseChatAgentFunction(BaseChatAgent agent) {
        this.agent = agent;
    }

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {
        final ChatRequest request = ChatRequest.newBuilder()
                .model(caller.request().model())
                .addMessage(Message.ofUser(parameter.input()))
                .build();
        return agent.async(request)
                .thenApply(response -> response.output().best().message().text())
                .thenApply(Result::new)
                .exceptionally(ex ->
                        new Result("智能体函数执行失败：" + ex.getLocalizedMessage())
                                .prompt("请检查以下几点：\n" +
                                        "- 输入的内容使用了完整的、正确的附件信息\n" +
                                        "- 输入的内容不存在假设内容"));
    }

    @Value
    @Accessors(fluent = true)
    @Jacksonized
    @Builder
    public static class Parameter {

        @JsonProperty(required = true)
        @JsonPropertyDescription("该参数用于详细描述您希望执行的具体任务或查询。\n" +
                                 "- 请包括所有必要的信息和参数，以便能够准确理解您的意图并执行相应的操作。\n" +
                                 "- 如果需要的信息和参数在附件中，需要将附件内容完整带上。")
        String input;

    }

    @Data
    @Accessors(fluent = true, chain = true)
    public static class Result {

        @JsonPropertyDescription("返回结果")
        @JsonProperty
        String output;

        @JsonPropertyDescription("返回结果提示")
        @JsonProperty
        String prompt;

        public Result(String output) {
            this.output = output;
        }

    }

}
