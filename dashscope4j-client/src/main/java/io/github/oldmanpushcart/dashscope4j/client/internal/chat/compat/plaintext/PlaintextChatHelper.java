package io.github.oldmanpushcart.dashscope4j.client.internal.chat.compat.plaintext;

import io.github.oldmanpushcart.dashscope4j.client.chat.ChatRequest;

public class PlaintextChatHelper {

    public static PlaintextChatRequest toPlaintextChatRequest(ChatRequest request) {
        return (PlaintextChatRequest) PlaintextChatRequest.newBuilder()
                .model(request.model())
                .parameters(request.parameters())
                .messages(request.messages())
                .tools(request.tools())
                .build();
    }

}
