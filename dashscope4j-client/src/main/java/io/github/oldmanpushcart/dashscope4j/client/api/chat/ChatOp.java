package io.github.oldmanpushcart.dashscope4j.client.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.OpAsync;
import io.github.oldmanpushcart.dashscope4j.client.OpFlow;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.ChatOpImpl;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.net.http.HttpClient;

public interface ChatOp extends OpAsync<ChatRequest, ChatResponse>, OpFlow<ChatRequest, ChatResponse> {

    static Builder newBuilder() {
        return new ChatOpImpl.BuilderImpl();
    }

    interface Builder extends Buildable<ChatOp, Builder> {

        Builder ak(String ak);

        Builder http(HttpClient http);

    }

}
