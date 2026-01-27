package io.github.oldmanpushcart.dashscope4j.client.aigc.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcModelTags;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 增量输出结果拦截器
 * <p>
 * 部分模型限定了输出模式必须是流式增量输出，所以这里需要对这种模型输出进行兼容。
 * 党外部希望是全量输出时，屏蔽掉这类模型的特殊性。
 * </p>
 */
public class IncrementalOutputOnlyInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> request)
                || chain.type() != Type.FLOW
                || request.parameters().has(AigcParameterKeys.INCREMENTAL_OUTPUT, true)
                || !request.model().tags().contains(AigcModelTags.INCREMENTAL_OUTPUT_ONLY)) {
            return chain.proceed();
        }

        final var newRequest = AigcRequest.newBuilder(request)
                .addParameter(AigcParameterKeys.INCREMENTAL_OUTPUT, true)
                .build();

        return chain.proceed(newRequest)
                .thenApply(v -> {
                    //noinspection unchecked
                    return (Flow.Publisher<AigcResponse<Output>>) v;
                })
                .thenApply(publisher -> {
                    final var responseRef = new AtomicReference<AigcResponse<Output>>();
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
                                return new AigcResponse<>(
                                        request,
                                        response.uuid(),
                                        response.code(),
                                        response.desc(),
                                        response.usage(),
                                        response.output()
                                );
                            });
                });
    }

}
