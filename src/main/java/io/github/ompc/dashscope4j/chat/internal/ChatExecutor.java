package io.github.ompc.dashscope4j.chat.internal;

import io.github.ompc.dashscope4j.chat.ChatModel;
import io.github.ompc.dashscope4j.chat.ChatRequest;
import io.github.ompc.dashscope4j.chat.ChatResponse;
import io.github.ompc.dashscope4j.chat.message.Content;
import io.github.ompc.dashscope4j.internal.algo.AlgoExecutor;

import java.net.http.HttpClient;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class ChatExecutor extends AlgoExecutor<ChatModel, ChatRequest.Data, ChatRequest, ChatResponse.Data, ChatResponse> {

    public ChatExecutor(String sk, HttpClient http, Executor executor, boolean stream) {
        super(sk, http, executor, stream, ChatResponse.class);
    }

    @Override
    public CompletableFuture<ChatResponse> execute(ChatRequest request, Consumer<ChatResponse> consumer) {
        return fetchingChatRequest(request)
                .thenCompose(v -> super.execute(v, consumer));
    }

    // 异步获取聊天请求
    private CompletableFuture<ChatRequest> fetchingChatRequest(ChatRequest request) {
        final var features = request.data().messages().stream()
                .flatMap(message -> message.contents().stream())
                .map(Content::fetch)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(features)
                .thenApply(v -> request);
    }

    @Override
    public String toString() {
        return "dashscope://chat";
    }

}
