package io.github.oldmanpushcart.dashscope4j.internal.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.oldmanpushcart.dashscope4j.Constants;
import io.github.oldmanpushcart.dashscope4j.Exchange;
import io.github.oldmanpushcart.dashscope4j.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonUtils;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.github.oldmanpushcart.dashscope4j.internal.util.HttpUtils.loggingHttpRequest;
import static io.github.oldmanpushcart.dashscope4j.internal.util.HttpUtils.loggingHttpResponse;

public class ApiOpImpl implements ApiOp {

    private final String ak;
    private final OkHttpClient http;

    public ApiOpImpl(String ak, OkHttpClient http) {
        this.ak = ak;
        this.http = http;
    }

    private Request newDelegateHttpRequest(Request httpRequest, Consumer<Request.Builder> consumer) {
        final Request.Builder builder = new Request.Builder(httpRequest)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", String.format("Bearer %s", ak))
                .addHeader("X-DashScope-Client", Constants.VERSION);
        consumer.accept(builder);
        return builder.build();
    }

    private Request newDelegateHttpRequest(Request httpRequest) {
        return newDelegateHttpRequest(httpRequest, b -> {

        });
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse<?>>
    CompletionStage<R> executeAsync(T request) {

        final Request httpRequest = newDelegateHttpRequest(request.newHttpRequest());
        loggingHttpRequest(httpRequest);

        final CompletableFuture<R> completed = new CompletableFuture<>();
        http.newCall(httpRequest).enqueue(new Callback() {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException ex) {
                loggingHttpResponse(null, ex);
                completed.completeExceptionally(ex);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response httpResponse) throws IOException {
                loggingHttpResponse(httpResponse, null);
                final String bodyString = Objects.requireNonNull(httpResponse.body()).string();
                final R response = request.newResponseDecoder().apply(bodyString);
                if (response.isSuccess()) {
                    completed.complete(response);
                } else {
                    completed.completeExceptionally(new ApiException(response));
                }
            }

        });
        return completed;

    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse<?>>
    CompletionStage<Flowable<R>> executeFlow(T request) {

        final Request httpRequest = newDelegateHttpRequest(request.newHttpRequest(), builder ->
                builder.addHeader("X-DashScope-SSE", "enable"));
        loggingHttpRequest(httpRequest);

        final Flowable<R> flow = Flowable.create(emitter -> {

            final EventSource source = EventSources.createFactory(http).newEventSource(httpRequest, new EventSourceListener() {

                private volatile boolean opened = false;

                @Override
                public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
                    loggingHttpResponse(response, null);
                    this.opened = true;
                }

                @Override
                public void onEvent(@NotNull EventSource eventSource, @Nullable String id, @Nullable String type, @NotNull String data) {
                    try {
                        if ("result".equals(type) || "error".equals(type)) {
                            final R response = request.newResponseDecoder().apply(data);
                            if (response.isSuccess()) {
                                emitter.onNext(response);
                            } else {
                                throw new ApiException(response);
                            }
                        } else {
                            throw new IllegalStateException(String.format("Unexpected event type: %s", type));
                        }
                    } catch (Throwable cause) {
                        if (!emitter.isCancelled()) {
                            emitter.onError(cause);
                        }
                    }
                }

                @Override
                public void onFailure(@NotNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {

                    /*
                     * 如果opened=false，说明本次异常是建联异常。
                     * 需要记录HTTP日志
                     */
                    if (!opened) {
                        loggingHttpResponse(response, t);
                    }

                    if (!emitter.isCancelled()) {
                        emitter.onError(t);
                    }
                }

                @Override
                public void onClosed(@NotNull EventSource eventSource) {
                    if (!emitter.isCancelled()) {
                        emitter.onComplete();
                    }
                }

            });

            emitter.setDisposable(Disposable.fromAction(source::cancel));

        }, BackpressureStrategy.BUFFER);
        return CompletableFuture.completedFuture(flow);
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse<?>>
    CompletionStage<Exchange<T>> executeExchange(T request, Exchange.Mode mode, Exchange.Listener<T, R> listener) {
        final CompletableFuture<Exchange<T>> exchangeF = new CompletableFuture<>();
        final String uuid = UUID.randomUUID().toString();
        final Function<T, String> encoder = JacksonUtils::toJson;

        /*
         * Exchange的Response反序列化
         */
        final Function<String, R> decoder = s -> {

            final ObjectNode payloadNode = (ObjectNode) JacksonUtils.toNode(s);

            /*
             * 特殊处理应答报文：{"output":{}} -> {"request_id":"...","output":{}}
             * Exchange返回的数据格式其中是不包含Response所需的request_id的，而Response又被设计为不可变类。
             * 所以需要手动在应答报文中添加上request_id属性，让Response反序列化得以正确进行。
             */
            payloadNode.put("request_id", uuid);

            final String payloadJson = payloadNode.toString();
            return request.newResponseDecoder().apply(payloadJson);
        };

        final WebSocketListener wsListener = new ExchangeWebSocketListenerImpl<>(
                exchangeF,
                uuid,
                mode,
                listener,
                encoder,
                decoder
        );

        final Request httpRequest = newDelegateHttpRequest(request.newHttpRequest());
        loggingHttpRequest(httpRequest);

        http.newWebSocket(httpRequest, wsListener);
        return exchangeF;
    }

}
