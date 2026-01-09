package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.interceptor.InlineImageFilesInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.interceptor.OpenAiCompatInterceptor;
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
            new OpenAiCompatInterceptor(),
            new InlineImageFilesInterceptor(),
            new UploadFilesInterceptor()
    );

    private static final List<FlowInterceptor> flowInterceptors = interceptors.stream()
            .filter(FlowInterceptor.class::isInstance)
            .map(FlowInterceptor.class::cast)
            .toList();

    private static final List<AsyncInterceptor> asyncInterceptors = interceptors.stream()
            .filter(AsyncInterceptor.class::isInstance)
            .map(AsyncInterceptor.class::cast)
            .toList();

    public ChatOpImpl(DashscopeClient client, AsyncApi asyncApi, FlowApi flowApi) {
        this.asyncApi = InterceptionAsyncApi.group(client, asyncApi, asyncInterceptors);
        this.flowApi = InterceptionFlowApi.group(client, flowApi, flowInterceptors);
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
                        .thenApply(publisher -> new NewToolCallFlowHandler(this).apply(publisher));
        return new DeferredPublisher<>(supplier);
    }

}
