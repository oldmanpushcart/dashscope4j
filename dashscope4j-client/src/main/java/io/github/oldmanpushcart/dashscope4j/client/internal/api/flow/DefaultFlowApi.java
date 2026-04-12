package io.github.oldmanpushcart.dashscope4j.client.internal.api.flow;

import io.github.oldmanpushcart.dashscope4j.client.Constants;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.InternalContents;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.ExecutorService;

public class DefaultFlowApi implements FlowApi, InternalContents {

    private final String host;
    private final String ak;
    private final OkHttpClient http;
    private final Scheduler scheduler;

    public DefaultFlowApi(String host, String ak, OkHttpClient http, ExecutorService executor) {
        this.host = host;
        this.ak = ak;
        this.http = http;
        // 将 Executor 转换为 Reactor Scheduler
        this.scheduler = Schedulers.fromExecutor(executor);
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> Publisher<R> execute(T request) {

        Flux<R> flux = Flux.create(sink -> {

            final var httpRequest = new Request.Builder(request.toHttpRequest(host))
                    .addHeader(HTTP_HEADER_X_DASHSCOPE_CLIENT, Constants.VERSION)
                    .addHeader(HTTP_HEADER_AUTHORIZATION, "Bearer %s".formatted(ak))
                    .addHeader(HTTP_HEADER_X_DASHSCOPE_SSE, ENABLE)
                    .addHeader(HTTP_HEADER_X_DASHSCOPE_ASYNC, DISABLE)
                    .addHeader(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)
                    .build();

            final var listener = new EventSourceListener() {

                private volatile Response httpResponse;

                @Override
                public void onOpen(@NonNull EventSource source, @NonNull Response httpResponse) {

                    if (sink.isCancelled()) {
                        source.cancel();
                        return;
                    }
                    sink.onCancel(source::cancel);
                    this.httpResponse = httpResponse;

                }

                @Override
                public void onEvent(@NonNull EventSource source, @Nullable String id, @Nullable String type, @NonNull String data) {

                    // 修复 omni 模型结尾弄个 [DONE] 的问题
                    if ("[DONE]".equals(data)) {
                        return;
                    }

                    final var response = request.responseDecoder().apply(httpResponse, data);
                    if (!response.isSuccess()) {
                        throw new ApiException(response);
                    }

                    if (!sink.isCancelled()) {
                        sink.next(response);
                    }

                }

                @Override
                public void onClosed(@NonNull EventSource source) {

                    if (!sink.isCancelled()) {
                        sink.complete();
                    }

                }

                @Override
                public void onFailure(@NonNull EventSource source, @Nullable Throwable t, @Nullable Response httpResponse) {

                    // 上游已取消就没必要继续了
                    if (sink.isCancelled()) {
                        return;
                    }

                    /*
                     * 整个 SSE 的失败分为3种情况
                     * 1. 流处理过程中出现了异常，这种情况直接使用异常作为上游失败的通知即可
                     * 2. 服务端返回了非 2xx 状态码，这种情况使用响应体作为上游失败的通知
                     * 3. 其他情况，使用 IllegalStateException 作为上游失败的通知
                     */
                    if (null != t) {
                        sink.error(t);
                    } else if (null != httpResponse
                            && !httpResponse.isSuccessful()
                            && "application/json".equals(httpResponse.header(HTTP_HEADER_CONTENT_TYPE))) {

                        try {
                            final var stringResponseBody = httpResponse.body().string();
                            final var response = request.responseDecoder().apply(httpResponse, stringResponseBody);
                            if (!response.isSuccess()) {
                                throw new ApiException(response);
                            }
                        } catch (Throwable ex) {
                            sink.error(ex);
                        }

                    } else {
                        sink.error(new IllegalStateException("SSE failed!"));
                    }

                }

            };

            EventSources.createFactory(http)
                    .newEventSource(httpRequest, listener);

        }, FluxSink.OverflowStrategy.ERROR);
        
        // 在接收到数据后切换到自定义 scheduler
        return flux.publishOn(scheduler);


    }

}
