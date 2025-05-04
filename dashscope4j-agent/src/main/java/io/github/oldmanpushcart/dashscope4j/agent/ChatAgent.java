package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.client.OpAsync;
import io.github.oldmanpushcart.dashscope4j.client.OpFlow;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

/**
 * 智能体
 */
public interface ChatAgent extends OpAsync<ChatRequest, ChatResponse>, OpFlow<ChatRequest, ChatResponse> {

    FunctionToolBuilder newFunctionToolBuilder();

    interface FunctionToolBuilder extends Buildable<ChatFunctionTool, FunctionToolBuilder> {

        FunctionToolBuilder name(String name);

        FunctionToolBuilder summary(String summary);

        FunctionToolBuilder enableFlowBridge(boolean enabled);

    }

}
