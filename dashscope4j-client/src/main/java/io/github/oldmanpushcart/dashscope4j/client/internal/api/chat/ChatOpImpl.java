package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.AsyncApiExecutor;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.FlowApiExecutor;

import java.net.http.HttpClient;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

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
        return asyncApi.execute(endpoint, request)
                .thenCompose(new FunctionToolCallOpAsyncHandler(this));
    }

    @Override
    public Flow.Publisher<ChatResponse> flow(ChatRequest request) {
        final var endpoint = request.model().endpoint();
        final var publisher = flowApi.execute(endpoint, request);
        return new FunctionToolCallOpFlowHandler(this)
                .apply(publisher);
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
