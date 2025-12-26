package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.AsyncApiExecutor;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.FlowApiExecutor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.codec.AsyncFileBase64Encoder;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.DeferredPublisher;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Function;

public class ChatOpImpl implements ChatOp {

    private final AsyncApiExecutor asyncApi;
    private final FlowApiExecutor flowApi;

    public ChatOpImpl(AsyncApiExecutor asyncApi, FlowApiExecutor flowApi) {
        this.asyncApi = asyncApi;
        this.flowApi = flowApi;
    }

    @Override
    public CompletionStage<ChatResponse> async(ChatRequest request) {
        final var endpoint = request.model().endpoint();
        return CompletableFuture.completedStage(request)
                .thenCompose(this::processingEachContentForInlineFileMediaAsBase64)
                .thenCompose(r -> asyncApi.execute(endpoint, r))
                .thenCompose(new FunctionToolCallOpAsyncHandler(this));
    }

    @Override
    public Flow.Publisher<ChatResponse> flow(ChatRequest request) {
        final var endpoint = request.model().endpoint();
        final var publisherF = CompletableFuture.completedStage(request)
                .thenCompose(this::processingEachContentForInlineFileMediaAsBase64)
                .thenApply(r -> flowApi.execute(endpoint, r))
                .thenApply(publisher -> new FunctionToolCallOpFlowHandler(this).apply(publisher));
        return new DeferredPublisher<>(publisherF);
    }

    private static CompletionStage<ChatRequest> processingEachMessage(ChatRequest request, Function<Message, CompletionStage<Message>> operator) {
        return CompletableFutureUtils.sequentialMap(request.messages(), operator)
                .thenApply(newMessages ->
                        ChatRequest.newBuilder(request)
                                .messages(newMessages)
                                .build());
    }

    private static CompletionStage<ChatRequest> processingEachContent(ChatRequest request, Function<Content<?>, CompletionStage<Content<?>>> operator) {
        return processingEachMessage(request, message -> CompletableFutureUtils.sequentialMap(message.contents(), operator)
                .thenApply(newContents ->
                        new Message(message.role(), newContents)));
    }


    private CompletionStage<ChatRequest> processingEachContentForInlineFileMediaAsBase64(ChatRequest request) {
        return processingEachContent(request, new Function<>() {

            private static boolean isFileURI(URI resourceURI) {
                return "file".equalsIgnoreCase(resourceURI.getScheme());
            }

            @Override
            public CompletionStage<Content<?>> apply(Content<?> content) {
                if (content instanceof Content.Media media) {
                    return CompletableFutureUtils
                            .sequentialMap(media.data(), resourceURI -> {
                                if (!isFileURI(resourceURI)) {
                                    return CompletableFuture.completedStage(resourceURI);
                                }
                                final var path = Paths.get(resourceURI);
                                return AsyncFileBase64Encoder.encode(path)
                                        .thenApply(base64Str -> URI.create("data:;base64," + base64Str));
                            })
                            .thenApply(media::changeData);
                } else {
                    return CompletableFuture.completedStage(content);
                }
            }

        });
    }

    public static class BuilderImpl implements ChatOp.Builder {

        private String ak;
        private HttpClient http;

        @Override
        public Builder ak(String ak) {
            this.ak = ak;
            return this;
        }

        @Override
        public Builder http(HttpClient http) {
            this.http = http;
            return this;
        }

        @Override
        public ChatOp build() {
            final var async = new AsyncApiExecutor(ak, http);
            final var flow = new FlowApiExecutor(ak, http);
            return new ChatOpImpl(async, flow);
        }

    }

}
