package io.github.ompc.dashscope4j.chat.internal;

import io.github.ompc.dashscope4j.chat.ChatRequest;
import io.github.ompc.dashscope4j.chat.ChatResponse;
import io.github.ompc.dashscope4j.chat.message.Content;
import io.github.ompc.dashscope4j.internal.algo.AlgoExecutor;

import java.net.http.HttpClient;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

public class ChatExecutor extends AlgoExecutor<ChatRequest, ChatResponse> {

    public ChatExecutor(String sk, HttpClient http, Executor executor) {
        super(sk, http, executor, ChatResponse.class);
    }

    @Override
    public CompletableFuture<ChatResponse> async(ChatRequest request) {
        return fetchingChatRequest(request)
                .thenCompose(super::async);
    }

    @Override
    public CompletableFuture<Flow.Publisher<ChatResponse>> flow(ChatRequest request) {
        return fetchingChatRequest(request)
                .thenCompose(super::flow);
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
