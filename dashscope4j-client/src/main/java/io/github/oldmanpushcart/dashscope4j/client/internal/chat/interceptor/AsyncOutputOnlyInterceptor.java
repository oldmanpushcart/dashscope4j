package io.github.oldmanpushcart.dashscope4j.client.internal.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.chat.ChatModelTags;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.FlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class AsyncOutputOnlyInterceptor implements FlowInterceptor {

    @Override
    public CompletionStage<? extends Flow.Publisher<?>> intercept(Chain chain) {

        if (!(chain.request() instanceof ChatRequest request)) {
            return chain.proceed();
        }

        final var model = request.model();
        if (!model.tags().contains(ChatModelTags.ASYNC_OUTPUT_ONLY)) {
            return chain.proceed();
        }

        final var chatOp = chain.client().chat();

        return CompletableFuture.completedStage(FlowX.defer(() ->
                FlowX.fromCompletionStage(chatOp.async(request).thenApply(FlowX::just))));
    }

}
