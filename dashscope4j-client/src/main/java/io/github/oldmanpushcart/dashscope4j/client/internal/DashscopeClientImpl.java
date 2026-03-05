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
import io.github.oldmanpushcart.dashscope4j.client.internal.interceptor.OpenTelemetryContextInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.Constants;
import okhttp3.OkHttpClient;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

public class DashscopeClientImpl implements DashscopeClient {

    private final AsyncApi asyncApi;
    private final FlowApi flowApi;
    private final TaskApi taskApi;
    private final RealtimeApi realtimeApi;
    private final BaseOp baseOp;
    private final List<Interceptor> interceptors;

    private DashscopeClientImpl(Builder builder) {

        Objects.requireNonNull(builder, "builder must not be null!");
        Objects.requireNonNull(builder.ak, "ak must not be null!");
        Objects.requireNonNull(builder.host, "host must not be null!");
        Objects.requireNonNull(builder.http, "http must not be null!");

        final var host = builder.host;
        final var ak = builder.ak;
        final var http = builder.http;

        final var asyncApi = new DefaultAsyncApi(host, ak, http);
        final var flowApi = new DefaultFlowApi(host, ak, http);
        final var taskApi = new DefaultTaskApi(host, ak, http, asyncApi);
        final var realtimeApi = new DefaultRealtimeApi(host, ak, http);

        this.asyncApi = asyncApi;
        this.flowApi = flowApi;
        this.taskApi = taskApi;
        this.realtimeApi = realtimeApi;
        this.baseOp = new BaseOpImpl(this);
        this.interceptors = newInterceptors(builder);

    }

    /**
     * 构造拦截链
     *
     * @param builder 构建器
     * @return 拦截链
     */
    private static List<Interceptor> newInterceptors(Builder builder) {
        final var newInterceptors = new ArrayList<Interceptor>();

        // 根据距离用户最近优先原则，builder注入的拦截链要在系统自带拦截链之前
        if (null != builder.interceptors) {
            newInterceptors.addAll(builder.interceptors);
        }

        // 最后添加系统自带拦截链
        newInterceptors.addAll(List.of(
                new BridgeInterceptor(),
                new IncrementalOutputOnlyInterceptor(),
                new GeneralAigcInterceptor()
        ));
        
        return Collections.unmodifiableList(newInterceptors);
    }


    /**
     * 合并拦截链
     * <p>拦截链的合并原则为：越靠近用户逻辑的拦截链越优先执行。</p>
     * <p>
     * 基于合并原则，我们认为不同生命周期的拦截链最终排序为：
     *     <ul>
     *         <li>调用拦截链；通过{@link #async(ApiRequest, List)},{@link #flow(ApiRequest, List)}, {@link #task(ApiRequest, List)}传入</li>
     *         <li>请求拦截链；通过{@link ApiRequest#interceptors()}传入</li>
     *         <li>客户端拦截链；在{@link #interceptors}中定义</li>
     *     </ul>
     * </p>
     *
     * @param interceptors        调用拦截链
     * @param requestInterceptors 请求拦截链
     * @return 合并后的拦截链
     */
    private static List<Interceptor> mergeInterceptors(List<Interceptor> interceptors, List<Interceptor> requestInterceptors, List<Interceptor> clientInterceptors) {
        return Stream.of(interceptors, requestInterceptors, clientInterceptors)
                .map(v -> Optional.ofNullable(v).orElseGet(List::of))
                .flatMap(List::stream)
                .toList();
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> async(T request, List<Interceptor> interceptors) {

        final var merged = mergeInterceptors(interceptors, request.interceptors(), this.interceptors);
        final var asyncApi = merged.isEmpty()
                ? this.asyncApi
                : InterceptionAsyncApi.group(this, this.asyncApi, merged);

        return asyncApi.execute(request);
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> Publisher<R> flow(T request, List<Interceptor> interceptors) {

        final var merged = mergeInterceptors(interceptors, request.interceptors(), this.interceptors);
        final var flowApi = merged.isEmpty()
                ? this.flowApi
                : InterceptionFlowApi.group(this, this.flowApi, merged);

        return Flux.from(flowApi.execute(request));
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> task(T request, List<Interceptor> interceptors) {

        final var merged = mergeInterceptors(interceptors, request.interceptors(), this.interceptors);
        final var taskApi = merged.isEmpty()
                ? this.taskApi
                : InterceptionTaskApi.group(this, this.taskApi, merged);

        return taskApi.execute(request);
    }

    @Override
    public <I, O> CompletionStage<? extends Realtime.Connection> realtime(Realtime.Session<I, O> session, Realtime.Handler<I, O> handler) {
        return realtimeApi.realtime(session, handler);
    }

    @Override
    public BaseOp base() {
        return baseOp;
    }

    public static class Builder implements DashscopeClient.Builder {

        private String host = Constants.DEFAULT_HOST;
        private String ak;
        private OkHttpClient http;
        private boolean traceable;
        private List<Interceptor> interceptors;

        @Override
        public DashscopeClient.Builder host(String host) {
            this.host = host;
            return this;
        }

        @Override
        public DashscopeClient.Builder ak(String ak) {
            this.ak = ak;
            return this;
        }

        @Override
        public DashscopeClient.Builder http(OkHttpClient http) {
            this.http = http;
            return this;
        }

        @Override
        public DashscopeClient.Builder traceable(boolean traceable) {
            this.traceable = traceable;
            return this;
        }

        @Override
        public DashscopeClient.Builder interceptors(List<Interceptor> interceptors) {
            this.interceptors = interceptors;
            return this;
        }

        @Override
        public DashscopeClient build() {

            // 如果启用了追踪，则添加追踪拦截器
            if (traceable) {
                final var newInterceptors = new ArrayList<Interceptor>();
                newInterceptors.add(new OpenTelemetryContextInterceptor());
                if (null != this.interceptors) {
                    newInterceptors.addAll(this.interceptors);
                }
                this.interceptors = newInterceptors;
            }

            // 构建原始的客户端
            DashscopeClient client = new DashscopeClientImpl(this);

            // 如果启用了追踪，返回包装后的客户端
            if (traceable) {
                client = new TraceableDashscopeClientImpl(client);
            }

            return client;
        }

    }

}
