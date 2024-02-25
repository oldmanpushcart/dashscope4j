package io.github.ompc.internal.dashscope4j.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ompc.dashscope4j.Option;
import io.github.ompc.dashscope4j.chat.ChatModel;
import io.github.ompc.dashscope4j.chat.ChatRequest;
import io.github.ompc.dashscope4j.chat.ChatResponse;
import io.github.ompc.dashscope4j.chat.message.Message;
import io.github.ompc.internal.dashscope4j.algo.AlgoRequestImpl;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ChatRequestImpl extends AlgoRequestImpl<ChatResponse> implements ChatRequest {

    ChatRequestImpl(ChatModel model, Input input, Option option, Duration timeout) {
        super(
                model,
                model.isText() ? backwardTextInput(input) : input,
                option,
                timeout,
                ChatResponseImpl.class
        );
    }

    @Override
    public String toString() {
        return "dashscope://chat";
    }

    // 将多模态模式的对话请求格式转换为文本模式的对话请求的格式
    private static Map<Object, Object> backwardTextInput(Input input) {
        return new HashMap<>() {{
            put("messages", new ArrayList<>() {{
                input.messages().forEach(message -> add(new HashMap<>() {{
                    put("role", message.role());
                    put("content", message.text());
                }}));
            }});
        }};
    }

    record InputImpl(
            @JsonProperty("messages")
            List<Message> messages
    ) implements Input {

    }

}
