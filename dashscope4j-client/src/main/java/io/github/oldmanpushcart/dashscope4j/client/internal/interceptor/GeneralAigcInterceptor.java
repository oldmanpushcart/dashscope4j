package io.github.oldmanpushcart.dashscope4j.client.internal.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.GeneralAigcModel;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.task.Task;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.DataURI;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通用模型拦截器
 * <p>
 * 处理通用模型的请求，比如{@code inline}、{@code upload}等
 * </p>
 */
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
                        processMap(inputMap, value -> {
                            if (value instanceof File file) {
                                return chain.client().base().store().upload(file.toURI(), model)
                                        .thenApply(v -> v);
                            }
                            if (value instanceof Path path) {
                                return chain.client().base().store().upload(path.toUri(), model)
                                        .thenApply(v -> v);
                            }
                            if (value instanceof URI uri && IOUtils.isFileURI(uri)) {
                                return chain.client().base().store().upload(uri, model)
                                        .thenApply(v -> v);
                            }
                            return CompletableFuture.completedStage(value);
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
                        processMap(inputMap, value -> {
                            if (value instanceof File file) {
                                return DataURI.from(file)
                                        .asyncToURI()
                                        .thenApply(v -> v);
                            }
                            if (value instanceof Path path) {
                                return DataURI.from(path)
                                        .asyncToURI()
                                        .thenApply(v -> v);
                            }
                            if (value instanceof URI uri && IOUtils.isFileURI(uri)) {
                                return DataURI.from(Paths.get(uri))
                                        .asyncToURI()
                                        .thenApply(v -> v);
                            }
                            return CompletableFuture.completedStage(value);
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private CompletionStage<Map> processMap(Map map, Function<Object, CompletionStage<Object>> operator) {
        final Set<Map.Entry> entries = map.entrySet();
        return CompletableFutureUtils
                .sequentialMap(entries, entry -> {

                    final var value = entry.getValue();

                    if (value instanceof Map subMap) {
                        return processMap(subMap, operator)
                                .thenApply(v -> new AbstractMap.SimpleEntry<>(entry.getKey(), v));
                    }

                    if (value instanceof Collection subList) {
                        return processList(subList, operator)
                                .thenApply(v -> new AbstractMap.SimpleEntry<>(entry.getKey(), v));
                    }

                    if (value.getClass().isArray()) {
                        return processList(Arrays.asList((Object[]) value), operator)
                                .thenApply(v -> new AbstractMap.SimpleEntry<>(entry.getKey(), v));
                    }

                    return operator.apply(value)
                            .thenApply(v -> new AbstractMap.SimpleEntry<>(entry.getKey(), v));

                })
                .thenApply(newEntries -> newEntries.stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        )));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private CompletionStage<List> processList(Collection list, Function<Object, CompletionStage<Object>> operator) {
        return CompletableFutureUtils
                .sequentialMap(list, item -> {

                    if (item == null) {
                        return CompletableFuture.completedFuture(null);
                    }

                    if (item instanceof Map subMap) {
                        return processMap(subMap, operator)
                                .thenApply(v -> v);
                    }

                    if (item instanceof Collection subList) {
                        return processList(subList, operator)
                                .thenApply(v -> v);
                    }

                    if (item.getClass().isArray()) {
                        return processList(Arrays.asList((Object[]) item), operator)
                                .thenApply(v -> v);
                    }

                    return operator.apply(item);

                });
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
