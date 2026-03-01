package io.github.oldmanpushcart.dashscope4j.client.internal.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.task.Task;
import io.github.oldmanpushcart.dashscope4j.client.util.tracer.Tracer;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

public class TraceInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        final var spanOpt = Tracer.instance.current();
        if (spanOpt.isEmpty()) {
            return chain.proceed();
        }

        final var request = chain.request();
        final var span = spanOpt.get();

        if (request instanceof AigcRequest<?, ?> aigcRequest) {
            final var model = aigcRequest.model();
            span.property("model", model.name());
        }

        return chain.proceed()
                .thenApply(r -> {

                    final var type = chain.type();

                    // async
                    if (type == Type.ASYNC) {
                        final var response = (ApiResponse) r;
                        span.property("uuid", response.uuid());
                        return r;
                    }

                    // flow
                    if (type == Type.FLOW) {
                        //noinspection unchecked
                        final var flow = (Publisher<ApiResponse>) r;
                        final var done = new AtomicBoolean(false);
                        return Flux.from(flow)
                                .doOnNext(response -> {
                                    if (!done.get()) {
                                        span.property("uuid", response.uuid());
                                        done.set(true);
                                    }
                                });
                    }

                    // task
                    if (type == Type.TASK) {
                        //noinspection unchecked
                        final var half = (Task.Half<ApiResponse>) r;
                        return (Task.Half<ApiResponse>) strategy ->
                                half.waitingFor(strategy)
                                        .thenApply(response -> {
                                            span.property("uuid", response.uuid());
                                            return response;
                                        });
                    }

                    return r;
                });

    }

}
