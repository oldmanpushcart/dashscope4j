package io.github.oldmanpushcart.dashscope4j.client.internal;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.api.task.Task;
import io.github.oldmanpushcart.dashscope4j.client.base.BaseOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.async.AsyncApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.async.DefaultAsyncApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.async.InterceptionAsyncApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.flow.DefaultFlowApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.flow.FlowApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.flow.InterceptionFlowApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.realtime.DefaultRealtimeApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.realtime.RealtimeApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.task.DefaultTaskApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.task.InterceptionTaskApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.task.TaskApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.BaseOpImpl;
import io.github.oldmanpushcart.dashscope4j.client.internal.interceptor.BridgeInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.interceptor.GeneralAigcInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.interceptor.IncrementalOutputOnlyInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.interceptor.TraceInterceptor;
import io.github.oldmanpushcart.dashscope4j.common.util.flow.FlowX;
import io.github.oldmanpushcart.dashscope4j.client.util.tracer.Tracer;
import io.github.oldmanpushcart.dashscope4j.common.Constants;
import io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils;

import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.stream.Stream;

import static io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

public class DashscopeClientImpl implements DashscopeClient {

    private final BaseOp baseOp;
    private final AsyncApi asyncApi;
    private final FlowApi flowApi;
    private final TaskApi taskApi;
    private final RealtimeApi realtimeApi;

    private static final List<Interceptor> globalInterceptors = List.of(
            new BridgeInterceptor(),
            new IncrementalOutputOnlyInterceptor(),
            new GeneralAigcInterceptor(),
            new TraceInterceptor()
    );

    private DashscopeClientImpl(Builder builder) {
        final var host = requireNonBlankString(builder.host, "host must not be blank!");
        final var ak = CheckUtils.requireNonBlankString(builder.ak, "ak must not be blank!");
        final var http = requireNonNull(builder.http, "http must not be null!");

        final var config = Config.newBuilder()
                .host(host)
                .ak(ak)
                .httpConnectTimeout(builder.httpConnectTimeout)
                .httpTimeout(builder.httpTimeout)
                .build();

        final var asyncApi = new DefaultAsyncApi(config, http);
        final var flowApi = new DefaultFlowApi(config, http);
        final var taskApi = new DefaultTaskApi(config, http, asyncApi);
        final var realtimeApi = new DefaultRealtimeApi(config, http);

        this.asyncApi = asyncApi;
        this.flowApi = flowApi;
        this.taskApi = taskApi;
        this.realtimeApi = realtimeApi;
        this.baseOp = new BaseOpImpl(this);

    }

    private static List<Interceptor> mergeInterceptors(List<Interceptor> interceptors, List<Interceptor> requestInterceptors) {
        return Stream.of(globalInterceptors, interceptors, requestInterceptors)
                .map(v -> Optional.ofNullable(v).orElseGet(List::of))
                .flatMap(List::stream)
                .toList();
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> async(T request, List<Interceptor> interceptors) {

        final var merged = mergeInterceptors(interceptors, request.interceptors());
        final var asyncApi = merged.isEmpty()
                ? this.asyncApi
                : InterceptionAsyncApi.group(this, this.asyncApi, merged);

        //noinspection resource
        final var scope = Tracer.instance.enter("async");
        return asyncApi.execute(request)
                .whenComplete((r, ex) -> {
                    final var span = scope.restore();
                    if (null == ex) {
                        span.success();
                    } else {
                        span.failure(ex);
                    }
                    scope.close();
                });
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> Flow.Publisher<R> flow(T request, List<Interceptor> interceptors) {

        final var merged = mergeInterceptors(interceptors, request.interceptors());
        final var flowApi = merged.isEmpty()
                ? this.flowApi
                : InterceptionFlowApi.group(this, this.flowApi, merged);

        //noinspection resource
        final var scope = Tracer.instance.enter("flow");
        return FlowX.fromPublisher(flowApi.execute(request))
                .doOnComplete(() -> {
                    scope.restore().success();
                    scope.close();
                })
                .doOnError(ex -> {
                    scope.restore().failure(ex);
                    scope.close();
                });
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> task(T request, List<Interceptor> interceptors) {

        final var merged = mergeInterceptors(interceptors, request.interceptors());
        final var taskApi = merged.isEmpty()
                ? this.taskApi
                : InterceptionTaskApi.group(this, this.taskApi, merged);

        //noinspection resource
        final var scope = Tracer.instance.enter("task");
        return taskApi.execute(request)
                .whenComplete((r, ex) -> {
                    scope.restore();
                    if (null != ex) {
                        scope.span()
                                .failure()
                                .property("exception", ex.getMessage());
                        scope.close();
                    }
                })
                .thenApply(half -> (Task.Half<R>) strategy -> half.waitingFor(strategy)
                        .whenComplete((r, ex) -> {
                            final var span = scope.restore();
                            if (null == ex) {
                                span.success();
                            } else {
                                span.failure(ex);
                            }
                            scope.close();
                        }));
    }

    @Override
    public <I, O> CompletionStage<? extends Realtime.Connection> realtime(Realtime.Session<I, O> session, Realtime.Handler<I, O> handler) {
        //noinspection resource
        final var scope = Tracer.instance.enter("realtime");
        scope.span()
                .property("type", "realtime")
                .property("model", String.valueOf(session.model()));
        return realtimeApi.realtime(session, new Realtime.Handler<>() {
                    @Override
                    public void onOpen(Realtime.Emitter<I> emitter) {
                        scope.restore();
                        handler.onOpen(emitter);
                    }

                    @Override
                    public CompletionStage<Void> onData(O output) {
                        scope.restore();
                        return handler.onData(output);
                    }

                    @Override
                    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
                        scope.restore();
                        return handler.onBinary(buffer);
                    }

                    @Override
                    public void onClosed(Throwable ex) {
                        scope.restore();
                        if (null != ex) {
                            scope.span()
                                    .failure()
                                    .property("exception", ex.getMessage());
                        } else {
                            scope.span().success();
                        }
                        scope.close();
                        handler.onClosed(ex);
                    }
                })
                .whenComplete((r, ex) -> {
                    scope.restore();
                    if (null != ex) {
                        scope.span()
                                .failure()
                                .property("exception", ex.getMessage());
                    }
                });
    }


    @Override
    public BaseOp base() {
        return baseOp;
    }


    public static class Builder implements DashscopeClient.Builder {

        private String host = Constants.DEFAULT_HOST;
        private String ak;
        private HttpClient http;
        private Duration httpConnectTimeout;
        private Duration httpTimeout;

        @Override
        public Builder host(String host) {
            this.host = requireNonBlankString(host, "host must not be blank!");
            return this;
        }

        @Override
        public Builder ak(String ak) {
            this.ak = requireNonBlankString(ak, "ak must not be blank!");
            return this;
        }

        @Override
        public Builder http(HttpClient http) {
            this.http = requireNonNull(http, "http must not be null!");
            return this;
        }

        @Override
        public DashscopeClient.Builder httpConnectTimeout(Duration httpConnectTimeout) {
            this.httpConnectTimeout = requireNonNull(httpConnectTimeout, "httpConnectTimeout must not be null!");
            return this;
        }

        @Override
        public DashscopeClient.Builder httpTimeout(Duration httpTimeout) {
            this.httpTimeout = requireNonNull(httpTimeout, "httpTimeout must not be null!");
            return this;
        }

        @Override
        public DashscopeClient build() {
            return new DashscopeClientImpl(this);
        }

    }

}
