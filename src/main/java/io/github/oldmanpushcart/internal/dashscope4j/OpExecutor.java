package io.github.oldmanpushcart.internal.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.Constants;
import io.github.oldmanpushcart.dashscope4j.api.ApiException;
import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.api.ApiResponse;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.internal.dashscope4j.util.HttpUtils.loggingHttpRequest;
import static io.github.oldmanpushcart.internal.dashscope4j.util.HttpUtils.loggingHttpResponse;

public class OpExecutor {

    private static final MediaType APPLICATION_JSON = MediaType.get("application/json");
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String ak;
    private final OkHttpClient http;

    public OpExecutor(String ak, OkHttpClient http) {
        this.ak = ak;
        this.http = http;
    }

    @AllArgsConstructor
    private static class FutureCallback implements Callback {

        private final CompletableFuture<String> future;

        @Override
        public void onFailure(@NotNull Call call, @NotNull IOException ex) {
            loggingHttpResponse(null, ex);
            future.completeExceptionally(ex);
        }

        @Override
        public void onResponse(@NotNull Call call, @NotNull Response httpResponse) throws IOException {
            loggingHttpResponse(httpResponse, null);
            if (httpResponse.isSuccessful()) {
                final ResponseBody responseBody = httpResponse.body();
                if (Objects.nonNull(responseBody)) {
                    future.complete(responseBody.string());
                } else {
                    future.complete(null);
                }
            } else {
                future.completeExceptionally(new IOException(String.format("Unexpected code: %s",
                        httpResponse.code()
                )));
            }
        }

    }

    private Headers newHeaders(ApiRequest<?, ?> request) {
        final Headers.Builder builder = new Headers.Builder();
        builder.add("Content-Type", "application/json");
        builder.add("Authorization", String.format("Bearer %s", ak));
        builder.add("X-DashScope-Client", Constants.VERSION);
        request.headers().forEach(builder::add);
        return builder.build();
    }

    public <R extends ApiResponse<?>> CompletionStage<R> executeAsync(ApiRequest<?, R> request) {
        final String requestJson = JacksonUtils.toJson(request);
        final Request httpRequest = new Request.Builder()
                .url(request.model().remote().toString())
                .headers(newHeaders(request))
                .post(RequestBody.create(requestJson, APPLICATION_JSON))
                .build();

        loggingHttpRequest(httpRequest);
        logger.debug("dashscope://async/{} >>> {}", request.model().name(), requestJson);

        final CompletableFuture<String> future = new CompletableFuture<>();
        http.newCall(httpRequest).enqueue(new FutureCallback(future));
        return future
                .whenComplete((r, ex) -> logger.debug("dashscope://async/{} <<< {}", request.model().name(), r, ex))
                .thenApply(responseJson -> JacksonUtils.toObject(responseJson, request.responseType()))
                .thenApply(response -> {
                    if (!response.ret().isSuccess()) {
                        throw new ApiException(response);
                    }
                    return response;
                });
    }

    public <R extends ApiResponse<?>> CompletionStage<Flowable<R>> executeFlow(ApiRequest<?, R> request) {
        final String requestJson = JacksonUtils.toJson(request);
        logger.debug("dashscope://flow/{} >>> {}", request.model().name(), requestJson);
        final Request httpRequest = new Request.Builder()
                .url(request.model().remote().toString())
                .headers(newHeaders(request))
                .addHeader("X-DashScope-SSE", "enable")
                .post(RequestBody.create(requestJson, APPLICATION_JSON))
                .build();

        final Flowable<R> flow = Flowable.create(emitter -> {

            final EventSource source = EventSources.createFactory(http).newEventSource(httpRequest, new EventSourceListener() {

                @Override
                public void onEvent(@NotNull EventSource eventSource, @Nullable String id, @Nullable String type, @NotNull String data) {
                    logger.debug("dashscope://flow/{} <<< {}|{}|{}", request.model().name(), id, type, data);
                    try {
                        if ("result".equals(type) || "error".equals(type)) {
                            final R response = JacksonUtils.toObject(data, request.responseType());
                            if (response.ret().isSuccess()) {
                                emitter.onNext(response);
                            } else {
                                throw new ApiException(response);
                            }
                        } else {
                            throw new IllegalStateException(String.format("Unexpected event type: %s", type));
                        }
                    } catch (Throwable cause) {
                        emitter.onError(cause);
                    }
                }

                @Override
                public void onFailure(@NotNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
                    emitter.onError(t);
                }

                @Override
                public void onClosed(@NotNull EventSource eventSource) {
                    emitter.onComplete();
                }

            });

            emitter.setCancellable(source::cancel);

        }, BackpressureStrategy.BUFFER);
        return CompletableFuture.completedFuture(flow);
    }

}