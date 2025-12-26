package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.AsyncApiExecutor;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.FlowApiExecutor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.DeferredPublisher;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content.Type.*;
import static io.github.oldmanpushcart.dashscope4j.common.util.CommonUtils.isIn;
import static io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils.sequentialMap;
import static java.nio.charset.StandardCharsets.US_ASCII;

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
                .thenCompose(this::processingEachContentForInlineLocalMediaAsBase64)
                .thenCompose(r -> asyncApi.execute(endpoint, r))
                .thenCompose(new FunctionToolCallOpAsyncHandler(this));
    }

    @Override
    public Flow.Publisher<ChatResponse> flow(ChatRequest request) {
        final var endpoint = request.model().endpoint();
        final var publisherF = CompletableFuture.completedStage(request)
                .thenCompose(this::processingEachContentForInlineLocalMediaAsBase64)
                .thenApply(r-> flowApi.execute(endpoint, r))
                .thenApply(publisher -> new FunctionToolCallOpFlowHandler(this).apply(publisher));
        return new DeferredPublisher<>(publisherF);
    }

    private static CompletionStage<ChatRequest> processingEachMessage(ChatRequest request, Function<Message, CompletionStage<Message>> operator) {
        return sequentialMap(request.messages(), operator)
                .thenApply(newMessages ->
                        ChatRequest.newBuilder(request)
                                .messages(newMessages)
                                .build());
    }

    private static CompletionStage<ChatRequest> processingEachContent(ChatRequest request, Function<Content<?>, CompletionStage<Content<?>>> operator) {
        return processingEachMessage(request, message -> sequentialMap(message.contents(), operator)
                .thenApply(newContents ->
                        new Message(message.role(), newContents)));
    }


    private CompletionStage<ChatRequest> processingEachContentForInlineLocalMediaAsBase64(ChatRequest request) {
        return processingEachContent(request, new Function<Content<?>, CompletionStage<Content<?>>>() {

            private static final int BUFFER_SIZE = 1024;

            private static String toBase64(URI resourceURI) throws IOException {
                try (final var input = resourceURI.toURL().openStream();
                     final var output = new ByteArrayOutputStream();
                     final var base64Output = Base64.getEncoder().wrap(output)) {
                    final var buffer = new byte[BUFFER_SIZE];
                    int readded;
                    while ((readded = input.read(buffer)) != -1) {
                        base64Output.write(buffer, 0, readded);
                    }
                    return output.toString(US_ASCII);
                }
            }

            private static boolean isFileURI(URI resourceURI) {
                return "file".equalsIgnoreCase(resourceURI.getScheme());
            }

            @Override
            public CompletionStage<Content<?>> apply(Content<?> content) {
                try {

                    if (isIn(content.type(), IMAGE, AUDIO, VIDEO)
                            && content.data() instanceof URI resourceURI
                            && isFileURI(resourceURI)) {
                        final var newResrouceURI = URI.create("data:;base64," + toBase64(resourceURI));
                        final var newContent = switch (content.type()) {
                            case IMAGE -> Content.ofImage(newResrouceURI);
                            case AUDIO -> Content.ofAudio(newResrouceURI);
                            case VIDEO -> Content.ofVideo(newResrouceURI);
                            default -> content;
                        };
                        return CompletableFuture.completedStage(newContent);
                    } else {
                        return CompletableFuture.completedStage(content);
                    }

                } catch (IOException ioEx) {
                    return CompletableFuture.failedStage(ioEx);
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
