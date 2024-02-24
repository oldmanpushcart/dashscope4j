package io.github.ompc.dashscope4j.internal.chat;

import io.github.ompc.dashscope4j.Model;
import io.github.ompc.dashscope4j.Option;
import io.github.ompc.dashscope4j.chat.ChatRequest;
import io.github.ompc.dashscope4j.chat.ChatResponse;
import io.github.ompc.dashscope4j.internal.algo.AlgoRequestImpl;

import java.time.Duration;

final class ChatRequestImpl extends AlgoRequestImpl<ChatResponse> implements ChatRequest {

    ChatRequestImpl(Model model, Input input, Option option, Duration timeout) {
        super(model, input, option, timeout, ChatResponseImpl.class);
    }

    @Override
    public String toString() {
        return "dashscope://chat";
    }

}
