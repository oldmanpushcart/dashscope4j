package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.FlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModelTags;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

public class IncrementalOutputOnlyInterceptor implements FlowInterceptor {

    @Override
    public CompletionStage<? extends Flow.Publisher<?>> intercept(Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?, ?> aigcRequest)
                || !(aigcRequest.parameters().has(ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT, true))
                || !(aigcRequest.model() instanceof ChatModel model)
                || !model.tags().contains(ChatModelTags.INCREMENTAL_OUTPUT_ONLY)) {
            return chain.proceed();
        }

        //noinspection unchecked
        final var chatRequest = (AigcRequest<Input, Output, ChatModel>) aigcRequest;

        final var newChatRequest = AigcRequest.newBuilder(chatRequest.model())
                .addParameter(ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT, true)
                .build();

        return chain.proceed(newChatRequest)
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
                                        aigcRequest,
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
