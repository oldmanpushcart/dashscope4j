package io.github.oldmanpushcart.dashscope4j.client.internal.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.GeneralAigcModel;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GeneralAigcInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        /*
         * 只处理 GeneralAigcModel 模型的请求
         */
        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof GeneralAigcModel model)) {
            return chain.proceed();
        }

        final var request = aigcRequest.as(model);

        return null;
    }

    private CompletionStage<AigcRequest<?, ?>> processUploadEnabled(Chain chain, final AigcRequest<Map<String, Object>, Map<String, Object>> request) {
        final var model = (GeneralAigcModel) request.model();
        if (!model.uploadEnabled()) {
            return CompletableFuture.completedFuture(request);
        }
        final var inputMap = request.input();
        return processInputMap(inputMap, entry -> {
            final var key = entry.getKey();
            final var value = entry.getValue();
            if(value instanceof Map<?, ?>) {
                return processInputMap()
            }
        });
    }

    private CompletionStage<Map<Object, Object>> processInputMap(
            Map<Object, Object> map,
            Function<Map.Entry<Object, Object>, CompletionStage<Map.Entry<Object, Object>>> operator
    ) {
        final var entries = map.entrySet();
        return CompletableFutureUtils.sequentialMap(entries, operator)
                .thenApply(newEntries -> newEntries.stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        )));
    }

}
