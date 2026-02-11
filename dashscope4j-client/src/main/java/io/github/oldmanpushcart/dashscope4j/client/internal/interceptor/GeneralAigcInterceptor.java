package io.github.oldmanpushcart.dashscope4j.client.internal.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.GeneralAigcModel;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.task.Task;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.DataURI;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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

        return CompletableFuture.completedStage(request)
                .thenCompose(r -> processUploadEnabled(chain, r))
                .thenCompose(this::processInlineEnabled)
                .thenCompose(chain::proceed)
                .thenApply(v -> {
                    if (chain.type() == Type.TASK) {
                        //noinspection unchecked
                        final var half = (Task.Half<AigcResponse<Map<String, Object>>>) v;
                        return cleanupTaskResponse(half);
                    }
                    return v;
                });
    }

    private CompletionStage<AigcRequest<Map<String, Object>, Map<String, Object>>> processUploadEnabled(Chain chain, AigcRequest<Map<String, Object>, Map<String, Object>> request) {
        final var model = (GeneralAigcModel) request.model();
        if (!model.uploadEnabled()) {
            return CompletableFuture.completedFuture(request);
        }
        return CompletableFuture.completedStage(request.input())
                .thenCompose(inputMap ->
                        processInputMap(inputMap, entry -> {
                            final var key = entry.getKey();
                            final var value = entry.getValue();
                            if (value instanceof File file) {
                                return chain.client().base().store().upload(file.toURI(), model)
                                        .thenApply(v -> new AbstractMap.SimpleEntry<>(key, v));
                            }
                            if (value instanceof Path path) {
                                return chain.client().base().store().upload(path.toUri(), model)
                                        .thenApply(v -> new AbstractMap.SimpleEntry<>(key, v));
                            }
                            if (value instanceof URI uri && IOUtils.isFileURI(uri)) {
                                return chain.client().base().store().upload(uri, model)
                                        .thenApply(v -> new AbstractMap.SimpleEntry<>(key, v));
                            }
                            return CompletableFuture.completedStage(entry);
                        }))
                .thenApply(newInputMap -> {
                    //noinspection unchecked
                    return (Map<String, Object>) newInputMap;
                })
                .thenApply(newInputMap ->
                        AigcRequest.newBuilder(request)
                                .input(newInputMap)
                                .build());
    }

    private CompletionStage<AigcRequest<Map<String, Object>, Map<String, Object>>> processInlineEnabled(AigcRequest<Map<String, Object>, Map<String, Object>> request) {
        final var model = (GeneralAigcModel) request.model();
        if (!model.inlineEnabled()) {
            return CompletableFuture.completedFuture(request);
        }
        return CompletableFuture.completedStage(request.input())
                .thenCompose(inputMap ->
                        processInputMap(inputMap, entry -> {
                            final var key = entry.getKey();
                            final var value = entry.getValue();
                            if (value instanceof File file) {
                                return DataURI.from(file)
                                        .asyncToURI()
                                        .thenApply(v -> new AbstractMap.SimpleEntry<>(key, v));
                            }
                            if (value instanceof Path path) {
                                return DataURI.from(path)
                                        .asyncToURI()
                                        .thenApply(v -> new AbstractMap.SimpleEntry<>(key, v));
                            }
                            if (value instanceof URI uri && IOUtils.isFileURI(uri)) {
                                return DataURI.from(Paths.get(uri))
                                        .asyncToURI()
                                        .thenApply(v -> new AbstractMap.SimpleEntry<>(key, v));
                            }
                            return CompletableFuture.completedStage(entry);
                        }))
                .thenApply(newInputMap -> {
                    //noinspection unchecked
                    return (Map<String, Object>) newInputMap;
                })
                .thenApply(newInputMap ->
                        AigcRequest.newBuilder(request)
                                .input(newInputMap)
                                .build());
    }

    private CompletionStage<Map<?, ?>> processInputMap(Map<?, ?> map, Function<Map.Entry<?, ?>, CompletionStage<Map.Entry<?, ?>>> operator) {
        final Set<Map.Entry<?, ?>> entries = Set.copyOf(map.entrySet());
        return CompletableFutureUtils
                .sequentialMap(entries, entry -> {
                    if (entry.getValue() instanceof Map<?, ?> subMap) {
                        return processInputMap(subMap, operator)
                                .thenApply(v -> new AbstractMap.SimpleEntry<>(entry.getKey(), v));
                    }
                    return operator.apply(entry);
                })
                .thenApply(newEntries -> newEntries.stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        )));
    }

    private Task.Half<AigcResponse<Map<String, Object>>> cleanupTaskResponse(Task.Half<AigcResponse<Map<String, Object>>> half) {
        return strategy -> half.waitingFor(strategy)
                .thenApply(response -> {
                    final var blacks = Set.of(
                            "task_id",
                            "task_status",
                            "submit_time",
                            "scheduled_time",
                            "end_time",
                            "task_metrics"
                    );
                    final var outputMap = response.output();
                    final var newOutputMap = new HashMap<String, Object>();
                    outputMap.forEach((k, v) -> {
                        if (!blacks.contains(k)) {
                            newOutputMap.put(k, v);
                        }
                    });
                    return new AigcResponse<>(
                            response.request(),
                            response.uuid(),
                            response.code(),
                            response.desc(),
                            response.usage(),
                            newOutputMap
                    );
                });
    }

}
