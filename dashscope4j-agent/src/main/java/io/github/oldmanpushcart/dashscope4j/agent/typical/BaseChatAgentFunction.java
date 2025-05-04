package io.github.oldmanpushcart.dashscope4j.agent.typical;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.ChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;
import lombok.Data;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.concurrent.CompletionStage;

class BaseChatAgentFunction
        implements ChatFunction<BaseChatAgentFunction.Parameter, BaseChatAgentFunction.Result> {

    private final ChatAgent agent;

    public BaseChatAgentFunction(BaseChatAgent agent) {
        this.agent = agent;
    }

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {
        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .addMessage(Message.ofUser(parameter.input()))
                .build();
        return agent.async(request)
                .thenApply(response -> response.output().best().message().text())
                .thenApply(Result::new)
                .exceptionally(ex -> {
                    final Result result = new Result("执行出错!")
                            .prompt(ex.getLocalizedMessage());
                    return result;
                });
    }

    @Value
    @Accessors(fluent = true)
    @Jacksonized
    @lombok.Builder
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
