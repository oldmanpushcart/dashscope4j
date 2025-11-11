package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.HttpAsyncExecutor;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.HttpFlowExecutor;

import java.net.http.HttpClient;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class ChatOpImpl implements ChatOp {

    private final HttpAsyncExecutor async;
    private final HttpFlowExecutor flow;

    public ChatOpImpl(HttpAsyncExecutor async, HttpFlowExecutor flow) {
        this.async = async;
        this.flow = flow;
    }

    @Override
    public CompletionStage<ChatResponse> async(ChatRequest request) {
        return async.execute(request)
                .thenCompose(new FunctionToolCallOpAsyncHandler(this));
    }

    @Override
    public Flow.Publisher<ChatResponse> flow(ChatRequest request) {
        final var publisher = flow.execute(request);
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
            final var async = new HttpAsyncExecutor(ak, http);
            final var flow = new HttpFlowExecutor(ak, http);
            return new ChatOpImpl(async, flow);
        }

    }

}
