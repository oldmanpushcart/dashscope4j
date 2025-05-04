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
    private final boolean flowBridgeEnabled;

    public BaseChatAgentFunction(BaseChatAgent agent, boolean flowBridgeEnabled) {
        this.agent = agent;
        this.flowBridgeEnabled = flowBridgeEnabled;
    }

    @Override
    public CompletionStage<Result> call(Caller caller, Parameter parameter) {
        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .addMessage(Message.ofUser(parameter.input()))
                .build();
        return flowBridgeEnabled
                ? callByFlowBridge(request)
                : callByAsyncDirect(request);
    }

    private CompletionStage<Result> callByAsyncDirect(ChatRequest request) {
        return agent.async(request)
                .thenApply(response -> {
                    final String text = response.output().best().message().text();
                    return new Result(text);
                });
    }

    private CompletionStage<Result> callByFlowBridge(ChatRequest request) {
        return agent.flow(request)
                .thenCompose(responseFlow -> responseFlow
                        .map(response -> response.output().best().message().text())
                        .reduce(new StringBuilder(), StringBuilder::append)
                        .toCompletionStage())
                .thenApply(StringBuilder::toString)
                .thenApply(Result::new);
    }

    @Value
    @Accessors(fluent = true)
    @Jacksonized
    @lombok.Builder
    public static class Parameter {

        @JsonProperty(required = true)
        @JsonPropertyDescription("该参数用于详细描述您希望执行的具体任务或查询。请包括所有必要的信息和参数，以便能够准确理解您的意图并执行相应的操作。输入应包含但不限于以下内容：\n" +
                                 "- 您希望执行的任务或查询的详细说明。\n" +
                                 "- 所需的任何特定参数或选项（例如：日期范围、数据类型等）。\n" +
                                 "- 对于需要处理的数据，请指定其来源或格式要求。\n" +
                                 "- 如果适用，请指出期望的输出格式或其他特殊指示。")
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
