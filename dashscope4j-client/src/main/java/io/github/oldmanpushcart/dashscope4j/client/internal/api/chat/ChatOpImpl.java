package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.AsyncApiExecutor;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.FlowApiExecutor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.function.Function;
import java.util.function.UnaryOperator;

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
                .thenApply(this::processingForBase64EncodeImageContent)
                .thenCompose(r -> asyncApi.execute(endpoint, r))
                .thenCompose(new FunctionToolCallOpAsyncHandler(this));
    }

    @Override
    public Flow.Publisher<ChatResponse> flow(ChatRequest request) {
        final var endpoint = request.model().endpoint();
        final var publisher = flowApi.execute(endpoint, request);
        return new FunctionToolCallOpFlowHandler(this)
                .apply(publisher);
    }

    private CompletionStage<ChatRequest> processingEachMessage(ChatRequest request, UnaryOperator<Message> operator) {
        final var newMessages = request.messages().stream()
                .map(operator)
                .toList();
        return ChatRequest.newBuilder(request)
                .messages(newMessages)
                .build();
    }

    private ChatRequest processingForBase64EncodeImageContent(ChatRequest request) {
        final var newMessages = request.messages().stream()
                .map(message -> {

                    if (message.getClass() != Message.class) {
                        return message;
                    }

                    final var newContents = message.contents().stream()
                            .map(content -> {
                                if (content.type() == Content.Type.IMAGE
                                        && content.data() instanceof URI imageURI
                                        && imageURI.getScheme().startsWith("file")) {
                                    try {
                                        final var imageBytes = IOUtils.toByteArray(imageURI);
                                        final var imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
                                        final var newImageURI = URI.create("data:;base64,%s".formatted(imageBase64));
                                        return Content.Media.ofImage(newImageURI);
                                    } catch (IOException ioEx) {
                                        throw new RuntimeException(ioEx);
                                    }
                                } else {
                                    return content;
                                }
                            })
                            .toList();
                    return new Message(message.role(), newContents);
                })
                .toList();
        return ChatRequest.newBuilder(request)
                .messages(newMessages)
                .build();
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
