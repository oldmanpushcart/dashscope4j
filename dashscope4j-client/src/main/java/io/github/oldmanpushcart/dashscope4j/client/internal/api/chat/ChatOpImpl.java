package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.interceptor.InlineImageFilesInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.interceptor.UploadFilesInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.*;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.DeferredPublisher;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Supplier;

public class ChatOpImpl implements ChatOp {

    private final AsyncApi asyncApi;
    private final FlowApi flowApi;

    private static final List<Interceptor> interceptors = List.of(
            new InlineImageFilesInterceptor(),
            new UploadFilesInterceptor()
    );

    public ChatOpImpl(DashscopeClient client, AsyncApi asyncApi, FlowApi flowApi) {
        this.asyncApi = InterceptionAsyncApi.group(client, asyncApi, interceptors);
        this.flowApi = InterceptionFlowApi.group(client, flowApi, interceptors);
    }

    @Override
    public CompletionStage<ChatResponse> async(ChatRequest request) {
        return CompletableFuture.completedStage(request)
                .thenCompose(asyncApi::execute)
                .thenCompose(new ToolCallHandler(this));
    }

    @Override
    public Flow.Publisher<ChatResponse> flow(ChatRequest request) {
        final Supplier<CompletionStage<Flow.Publisher<ChatResponse>>> supplier = () ->
                CompletableFuture.completedStage(request)
                        .thenApply(flowApi::execute)
                        .thenApply(publisher -> new ToolCallFlowHandler(this).apply(publisher));
        return new DeferredPublisher<>(supplier);
    }

}
