package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 对话请求处理函数工具调用
 */
class ProcessFunctionToolCallForChatInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        // 只处理对话请求
        if (!(chain.request() instanceof ChatRequest)) {
            return chain.process(chain.request());
        }

        return chain.process(chain.request())
                .thenCompose(v -> process(chain, v));
    }

    // 处理应答
    private CompletionStage<?> process(Chain chain, Object v) {

        final DashscopeClient client = chain.client();

        // 处理Async应答
        if (v instanceof ChatResponse) {
            return new FunctionToolCallOpAsyncHandler(client, client.chat())
                    .apply((ChatResponse) v);
        }

        // 处理Flow应答
        else if (v instanceof Flowable<?>) {
            @SuppressWarnings("unchecked") final Flowable<ChatResponse> responseFlow = (Flowable<ChatResponse>) v;
            final Flowable<ChatResponse> tcFlow = new FunctionToolCallOpFlowHandler(client, client.chat())
                    .apply(responseFlow);
            return CompletableFuture.completedFuture(tcFlow);
        }

        // 其他类型无法处理，直接放行
        else {
            return CompletableFuture.completedFuture(v);
        }

    }

}
