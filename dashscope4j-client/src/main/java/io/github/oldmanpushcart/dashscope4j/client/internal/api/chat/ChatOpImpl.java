package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.interceptor.InlineImageFilesInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.AsyncApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.FlowApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.InterceptionAsyncApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.InterceptionFlowApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.DeferredPublisher;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Supplier;

public class ChatOpImpl implements ChatOp {

    private final DashscopeClient client;
    private final AsyncApi asyncApi;
    private final FlowApi flowApi;

    private static final List<ApiInterceptor> interceptors = List.of(
            new InlineImageFilesInterceptor()
    );

    public ChatOpImpl(DashscopeClient client, AsyncApi asyncApi, FlowApi flowApi) {
        this.client = client;
        this.asyncApi = InterceptionAsyncApi.group(client, asyncApi, interceptors);
        this.flowApi = InterceptionFlowApi.group(client, flowApi, interceptors);
    }

    private URI toEndpoint(ChatRequest request) {
        final var model = request.model();
        final var host = client.host();
        return EndpointUtils
                .https(host, model.path());
    }

    @Override
    public CompletionStage<ChatResponse> async(ChatRequest request) {
        return CompletableFuture.completedStage(request)
                .thenCompose(r -> asyncApi.execute(toEndpoint(r), r))
                .thenCompose(new ToolCallHandler(this));
    }

    @Override
    public Flow.Publisher<ChatResponse> flow(ChatRequest request) {
        final Supplier<CompletionStage<Flow.Publisher<ChatResponse>>> supplier = () ->
                CompletableFuture.completedStage(request)
                        .thenApply(r -> flowApi.execute(toEndpoint(r), r))
                        .thenApply(publisher -> new ToolCallFlowHandler(this).apply(publisher));
        return new DeferredPublisher<>(supplier);
    }

}
