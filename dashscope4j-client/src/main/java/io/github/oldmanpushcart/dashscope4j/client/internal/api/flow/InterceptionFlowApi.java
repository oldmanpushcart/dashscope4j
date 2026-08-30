package io.github.oldmanpushcart.dashscope4j.client.internal.api.flow;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.PublisherUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class InterceptionFlowApi implements FlowApi {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DashscopeClient client;
    private final FlowApi delegate;
    private final Interceptor interceptor;

    public InterceptionFlowApi(DashscopeClient client, FlowApi delegate, Interceptor interceptor) {
        this.client = client;
        this.delegate = delegate;
        this.interceptor = interceptor;
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> Publisher<R> execute(T request) {
        return Flux.defer(() -> {

            final var chain = new Interceptor.Chain(
                    Interceptor.Type.FLOW,
                    client,
                    request,
                    r -> {
                        final var prefix = interceptor.getClass().getName();
                        logger.trace("{} >>> ENTER", prefix);
                        return CompletableFuture.completedStage(delegate.execute(r))
                                .whenComplete((u, ex) -> {
                                    logger.trace("{} <<< EXIT", prefix, ex);
                                })
                                .thenApply(p -> {
                                    return Flux.from(p)
                                            .doOnSubscribe(_u -> logger.trace("{}/flow >>> SUBSCRIBED!", prefix))
                                            .doOnError(ex-> logger.trace("{}/flow <<< ERROR!", prefix, ex))
                                            .doOnCancel(() -> logger.trace("{}/flow <<< CANCEL!", prefix))
                                            .doOnComplete(() -> logger.trace("{}/flow <<< COMPLETE!", prefix))
                                            .doOnTerminate(() -> logger.trace("{}/flow <<< TERMINATE!", prefix));
                                });
                    }
            );

            final var stage = interceptor.intercept(chain)
                    .thenApply(r -> {
                        //noinspection unchecked
                        return (Publisher<R>) r;
                    });

            return PublisherUtils.fromCancellableStage(stage);
        });
    }

    public static FlowApi group(DashscopeClient client, FlowApi delegate, List<Interceptor> interceptors) {

        /*
         * 这里需要对拦截器进行倒序处理，因为拦截器会进行逆序链式调用，因此需要先处理最外层的拦截器。
         * 这样就可以做到：排在最前边的拦截器最先被执行，符合人类设置的直接观感
         */
        final var cloneList = new ArrayList<>(interceptors);
        Collections.reverse(cloneList);

        FlowApi api = delegate;
        for (final var interceptor : cloneList) {
            api = new InterceptionFlowApi(client, api, interceptor);
        }
        return api;
    }

}
