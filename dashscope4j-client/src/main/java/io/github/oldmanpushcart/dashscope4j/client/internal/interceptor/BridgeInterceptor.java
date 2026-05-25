package io.github.oldmanpushcart.dashscope4j.client.internal.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.AigcModelTags;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.client.api.task.Task.WaitStrategies.always;
import static java.time.Duration.ofSeconds;

/**
 * 桥接拦截器
 */
public class BridgeInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> request)) {
            return chain.proceed();
        }

        final var model = request.model();

        // process async request
        if (chain.type() == Type.ASYNC) {

            // bridge async tp async
            if (model.tags().contains(AigcModelTags.RESPONSE_MODE_ASYNC)) {
                return chain.proceed();
            }

            // bridge async to flow
            else if (model.tags().contains(AigcModelTags.RESPONSE_MODE_FLOW)) {
                final var newRequest = AigcRequest.newBuilder(request)
                        .parameters(parameters -> {
                            parameters.put("incremental_output", true);
                            return parameters;
                        })
                        .tags(tags-> {
                            tags.add("bridge:a2f");
                            return tags;
                        })
                        .build();
                return Flux.from(chain.client().flow(newRequest))
                        .reduce(AigcResponse::accumulate)
                        .toFuture();
            }

            // bridge async to task
            else if (model.tags().contains(AigcModelTags.RESPONSE_MODE_TASK)) {
                final var newRequest = AigcRequest.newBuilder(request)
                        .tags(tags-> {
                            tags.add("bridge:a2t");
                            return tags;
                        })
                        .build();
                return chain.client().task(newRequest)
                        .thenCompose(half -> half.waitingFor(always(ofSeconds(1L))));
            }

        }

        // process flow request
        else if (chain.type() == Type.FLOW) {

            // bridge flow to flow
            if (model.tags().contains(AigcModelTags.RESPONSE_MODE_FLOW)) {
                return chain.proceed();
            }

            // bridge flow to async
            else if (model.tags().contains(AigcModelTags.RESPONSE_MODE_ASYNC)) {
                final var flow = Flux.defer(() -> {
                    final var newRequest = AigcRequest.newBuilder(request)

                            .parameters(parameters -> {

                                /*
                                 * 这里是修复 qwen-long 对增量输出的问题
                                 * 如果不设置 INCREMENTAL_OUTPUT 为 false，那么 qwen-long 会返回一个空的结果，导致无法获取到结果
                                 */
                                parameters.put("incremental_output", false);

                                return parameters;
                            })

                            .tags(tags-> {
                                tags.add("bridge:f2a");
                                return tags;
                            })

                            .build();
                    return Mono.fromCompletionStage(chain.client().async(newRequest));
                });
                return CompletableFuture.completedStage(flow);
            }

            // bridge flow to task
            else if (model.tags().contains(AigcModelTags.RESPONSE_MODE_TASK)) {
                final var flow = Flux.defer(() -> {
                    final var newRequest = AigcRequest.newBuilder(request)
                            .parameters(parameters -> {

                                /*
                                 * 这里是修复 qwen-long 对增量输出的问题
                                 * 如果不设置 INCREMENTAL_OUTPUT 为 false，那么 qwen-long 会返回一个空的结果，导致无法获取到结果
                                 */
                                parameters.put("incremental_output", false);

                                return parameters;
                            })

                            .tags(tags-> {
                                tags.add("bridge:f2t");
                                return tags;
                            })

                            .build();
                    final var stage = chain.client().task(newRequest)
                            .thenCompose(half -> half.waitingFor(always(ofSeconds(1L))));
                    return Mono.fromCompletionStage(stage);
                });
                return CompletableFuture.completedStage(flow);
            }

        }

        // process task request
        else if (chain.type() == Type.TASK) {

            // bridge task to task
            if (model.tags().contains(AigcModelTags.RESPONSE_MODE_TASK)) {
                return chain.proceed();
            }

            // bridge task to async or flow
            else if (model.tags().contains(AigcModelTags.RESPONSE_MODE_ASYNC)
                    || model.tags().contains(AigcModelTags.RESPONSE_MODE_FLOW)) {
                return CompletableFuture.failedStage(new UnsupportedOperationException("Task requests are not supported; model requires async or flow."));
            }

        }

        /*
         * 其他情况就不处理了，能走到这里的主要原因是
         * 1. 模型上没有任何响应模式限制标签
         * 2. 出现了新的响应模式但这里忘记修改
         */
        return chain.proceed();
    }

}
