package io.github.oldmanpushcart.dashscope4j.internal.api;

import io.github.oldmanpushcart.dashscope4j.Constants;
import io.github.oldmanpushcart.dashscope4j.Exchange;
import io.github.oldmanpushcart.dashscope4j.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonJsonUtils;
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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class ApiOpImpl implements ApiOp {

    private final String ak;
    private final OkHttpClient http;

    public ApiOpImpl(String ak, OkHttpClient http) {
        this.ak = ak;
        this.http = http;
    }

    private Request newDelegateHttpRequest(ApiRequest<?> request, Consumer<Request.Builder> consumer) {
        final Request httpRequest = request.newHttpRequest();
        final Request.Builder builder = new Request.Builder(httpRequest)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", String.format("Bearer %s", ak))
                .addHeader("X-DashScope-Client", Constants.VERSION);
        consumer.accept(builder);
        return builder.build();
    }

    private Request newDelegateHttpRequest(ApiRequest<?> request) {
        return newDelegateHttpRequest(request, b -> {

        });
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse<?>>
    CompletionStage<R> executeAsync(T request) {

        final Request httpRequest = newDelegateHttpRequest(request);

        final CompletableFuture<R> completed = new CompletableFuture<>();
        http.newCall(httpRequest).enqueue(new Callback() {

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException ex) {
                completed.completeExceptionally(ex);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response httpResponse) {
                try {
                    final String bodyJson = requireNonNull(httpResponse.body()).string();
                    final R response = request.newResponseDecoder().apply(httpResponse, bodyJson);
                    if (response.isSuccess()) {
                        completed.complete(response);
                    } else {
                        completed.completeExceptionally(new ApiException(response));
                    }
                } catch (Throwable ex) {
                    completed.completeExceptionally(ex);
                }
            }

        });
        return completed;

    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse<?>>
    CompletionStage<Flowable<R>> executeFlow(T request) {

        final Request httpRequest = newDelegateHttpRequest(request, builder ->
                builder.addHeader("X-DashScope-SSE", "enable"));

        final Flowable<R> flow = Flowable.create(emitter -> {

            final EventSource source = EventSources.createFactory(http).newEventSource(httpRequest, new EventSourceListener() {

                private volatile okhttp3.Response httpResponse;

                @Override
                public void onOpen(@NotNull EventSource eventSource, @NotNull Response httpResponse) {
                    this.httpResponse = httpResponse;
                }

                @Override
                public void onEvent(@NotNull EventSource eventSource, @Nullable String id, @Nullable String type, @NotNull String data) {
                    try {
                        if ("result".equals(type) || "error".equals(type)) {
                            final R response = request.newResponseDecoder().apply(httpResponse, data);
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
        final Function<T, String> encoder = JacksonJsonUtils::toJson;
        final BiFunction<okhttp3.Response, String, R> decoder = request.newResponseDecoder();

        final WebSocketListener wsListener = new ExchangeWebSocketListenerImpl<>(
                exchangeF,
                uuid,
                mode,
                listener,
                encoder,
                decoder
        );

        final Request httpRequest = newDelegateHttpRequest(request);
        http.newWebSocket(httpRequest, wsListener);
        return exchangeF;
    }

}
