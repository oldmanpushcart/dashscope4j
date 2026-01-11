package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.FlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.dashscope4j.common.util.CommonUtils.hasKeyValue;

public class IncrementalOutputOnlyInterceptor implements FlowInterceptor {

    @Override
    public CompletionStage<? extends Flow.Publisher<?>> intercept(Chain chain) {

        if (!(chain.request() instanceof ChatRequest request)) {
            return chain.proceed();
        }

        // 只处理增量输出的模型
        final var model = request.model();
        if (!hasKeyValue(model.features(), "incremental-output-only", "1")) {
            return chain.proceed();
        }

        // 只处理没有启用增量输出的情况
        if (request.parameters().has(ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT, true)) {
            return chain.proceed();
        }

        final var newRequest = ChatRequest.newBuilder(request)
                .parameter(ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT, true)
                .build();

        return chain.proceed(newRequest)
                .thenApply(v -> {
                    //noinspection unchecked
                    return (Flow.Publisher<ChatResponse>) v;
                })
                .thenApply(publisher -> {
                    final var responseRef = new AtomicReference<ChatResponse>();
                    return FlowX.fromPublisher(publisher)
                            .map(incrementalResponse -> {
                                var response = responseRef.get();
                                if (response == null) {
                                    response = incrementalResponse;
                                } else {
                                    response = response.accumulate(incrementalResponse);
                                }
                                responseRef.set(response);

                                /*
                                 * 因为后边需要串改 response 的返回结果为非增量输出，
                                 * 这里需要将原来的 request 还原回去（原来的 request 就是非增量输出），避免后续处理合并时出错
                                 */
                                return new ChatResponse(
                                        request,
                                        response.uuid(),
                                        response.code(),
                                        response.desc(),
                                        response.usage(),
                                        response.output()
                                );
                            })
                            .publisher();
                });
    }

}
