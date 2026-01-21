package io.github.oldmanpushcart.dashscope4j.client.internal.aigc.chat;

import io.github.oldmanpushcart.dashscope4j.client.*;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class ChatOpImpl implements ChatOp {

    private final DashscopeClient client;
    private final List<AsyncInterceptor> asyncInterceptors;
    private final List<TaskInterceptor> taskInterceptors;
    private final List<FlowInterceptor> flowInterceptors;

    public ChatOpImpl(Builder builder) {
        this.client = builder.client;
        this.asyncInterceptors = builder.interceptors.stream()
                .filter(AsyncInterceptor.class::isInstance)
                .map(AsyncInterceptor.class::cast)
                .toList();
        this.taskInterceptors = builder.interceptors.stream()
                .filter(TaskInterceptor.class::isInstance)
                .map(TaskInterceptor.class::cast)
                .toList();
        this.flowInterceptors = builder.interceptors.stream()
                .filter(FlowInterceptor.class::isInstance)
                .map(FlowInterceptor.class::cast)
                .toList();
    }

    @Override
    public CompletionStage<AigcResponse<Output>> async(AigcRequest<?, Output, ChatModel> request) {
        return async(request, List.of());
    }

    @Override
    public CompletionStage<AigcResponse<Output>> async(AigcRequest<?, Output, ChatModel> request, List<AsyncInterceptor> interceptors) {
        final var merged = new ArrayList<AsyncInterceptor>();
        merged.addAll(interceptors);
        merged.addAll(asyncInterceptors);
        return client.async(request, merged)
                .thenCompose(new ToolCallHandler(this));
    }

    @Override
    public CompletionStage<? extends Task.Half<AigcResponse<Output>>> task(AigcRequest<?, Output, ChatModel> request) {
        return task(request, List.of());
    }

    @Override
    public CompletionStage<? extends Task.Half<AigcResponse<Output>>> task(AigcRequest<?, Output, ChatModel> request, List<TaskInterceptor> interceptors) {
        final var merged = new ArrayList<TaskInterceptor>();
        merged.addAll(interceptors);
        merged.addAll(taskInterceptors);
        return client.task(request, merged);
    }

    @Override
    public Flow.Publisher<AigcResponse<Output>> flow(AigcRequest<?, Output, ChatModel> request) {
        return client.flow(request, flowInterceptors);
    }

    @Override
    public Flow.Publisher<AigcResponse<Output>> flow(AigcRequest<?, Output, ChatModel> request, List<FlowInterceptor> interceptors) {
        final var merged = new ArrayList<FlowInterceptor>();
        merged.addAll(interceptors);
        merged.addAll(flowInterceptors);
        return FlowX.fromPublisher(client.flow(request, merged))
                .transform(new ToolCallFlowHandler(this));
    }


    public static class Builder implements ChatOp.Builder {

        private DashscopeClient client;
        private final List<Interceptor> interceptors = new ArrayList<>();

        @Override
        public ChatOp.Builder interceptors(List<Interceptor> interceptors) {
            this.interceptors.clear();
            this.interceptors.addAll(interceptors);
            return this;
        }

        @Override
        public ChatOp.Builder addInterceptor(Interceptor interceptor) {
            this.interceptors.add(interceptor);
            return this;
        }

        @Override
        public ChatOp.Builder addInterceptors(List<Interceptor> interceptors) {
            this.interceptors.addAll(interceptors);
            return this;
        }

        @Override
        public ChatOp.Builder client(DashscopeClient client) {
            this.client = client;
            return this;
        }

        @Override
        public ChatOp build() {
            return new ChatOpImpl(this);
        }

    }

}
