package io.github.ompc.internal.dashscope4j.chat;

import io.github.ompc.dashscope4j.Option;
import io.github.ompc.dashscope4j.chat.ChatModel;
import io.github.ompc.dashscope4j.chat.ChatRequest;
import io.github.ompc.dashscope4j.chat.ChatResponse;
import io.github.ompc.internal.dashscope4j.base.algo.AlgoRequestImpl;

import java.time.Duration;

final class ChatRequestImpl extends AlgoRequestImpl<ChatResponse> implements ChatRequest {

    ChatRequestImpl(ChatModel model, Object input, Option option, Duration timeout) {
        super(model, input, option, timeout, ChatResponseImpl.class);
    }

    @Override
    public String toString() {
        return "dashscope://chat";
    }

}
