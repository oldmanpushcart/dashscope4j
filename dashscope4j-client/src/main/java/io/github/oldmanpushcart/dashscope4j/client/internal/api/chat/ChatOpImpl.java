package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.process.ChatRequestProcessor;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.process.FileToDataUriImageContentProcessor;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.AsyncApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.FlowApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.EndpointUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.DeferredPublisher;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class ChatOpImpl implements ChatOp {

    private final DashscopeClient client;
    private final AsyncApi asyncApi;
    private final FlowApi flowApi;
    private final ChatRequestProcessor chatRequestProcessor;

    public ChatOpImpl(DashscopeClient client, AsyncApi asyncApi, FlowApi flowApi) {
        this.client = client;
        this.asyncApi = asyncApi;
        this.flowApi = flowApi;
        this.chatRequestProcessor = newChatRequestProcessor();
    }

    private ChatRequestProcessor newChatRequestProcessor() {
        return ChatRequestProcessor.group(
                new FileToDataUriImageContentProcessor()
        );
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
                .thenCompose(chatRequestProcessor::process)
                .thenCompose(r -> asyncApi.execute(toEndpoint(r), r))
                .thenCompose(new FunctionToolCallOpAsyncHandler(this));
    }

    @Override
    public Flow.Publisher<ChatResponse> flow(ChatRequest request) {
        final var publisherF = CompletableFuture.completedStage(request)
                .thenCompose(chatRequestProcessor::process)
                .thenApply(r -> flowApi.execute(toEndpoint(r), r))
                .thenApply(publisher -> new FunctionToolCallOpFlowHandler(this).apply(publisher));
        return new DeferredPublisher<>(publisherF);
    }

}
