package io.github.ompc.dashscope4j.internal.api;

import io.github.ompc.dashscope4j.internal.api.http.HttpHeader;
import io.github.ompc.dashscope4j.internal.api.http.HttpResponseEventSubscriber;
import io.github.ompc.dashscope4j.internal.util.Buildable;

import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.ContentType.MIME_APPLICATION_JSON;
import static io.github.ompc.dashscope4j.internal.api.http.HttpHeader.ContentType.MIME_TEXT_EVENT_STREAM;
import static java.util.Objects.requireNonNull;

class ApiResponseBodyHandler<D extends ApiData, R extends ApiResponse<D, R>> implements HttpResponse.BodyHandler<R> {

    private final Function<String, R> deserializer;
    private final Consumer<R> consumer;
    private final BinaryOperator<R> accumulator;

    public ApiResponseBodyHandler(Builder<D, R> builder) {
        this.deserializer = requireNonNull(builder.deserializer);
        this.consumer = requireNonNull(builder.consumer);
        this.accumulator = requireNonNull(builder.accumulator);
    }

    @Override
    public HttpResponse.BodySubscriber<R> apply(HttpResponse.ResponseInfo info) {

        final var ct = HttpHeader.ContentType.parse(info.headers());
        final var charset = ct.charset();

        return switch (ct.mime()) {
            case MIME_APPLICATION_JSON -> newBlockSubscriber(info, charset);
            case MIME_TEXT_EVENT_STREAM -> newStreamSubscriber(charset);
            default -> throw new RuntimeException("Unsupported Content-Type: %s".formatted(ct.mime()));
        };

    }

    private HttpResponse.BodySubscriber<R> newBlockSubscriber(HttpResponse.ResponseInfo info, Charset charset) {
        return HttpResponse.BodySubscribers.fromSubscriber(
                HttpResponse.BodySubscribers.ofString(charset),
                subscriber -> {
                    final var body = subscriber.getBody().toCompletableFuture().join();
                    final var response = deserializer.apply(body);
                    if (!response.ret().isSuccess()) {
                        throw new ApiException(info.statusCode(), response.ret());
                    }
                    return response;
                }
        );
    }

    private HttpResponse.BodySubscriber<R> newStreamSubscriber(Charset charset) {
        final var responseRef = new AtomicReference<R>();
        return HttpResponse.BodySubscribers.fromSubscriber(
                new HttpResponseEventSubscriber(charset, event -> {
                    final var response = deserializer.apply(event.data());

                    // 检查返回结果是否异常
                    if (!response.ret().isSuccess()) {

                        // 解析HTTP状态：HTTP_STATUS/429
                        final var status = event.meta().stream()
                                .filter(meta -> meta.startsWith("HTTP_STATUS/"))
                                .map(meta -> Integer.parseInt(meta.substring("HTTP_STATUS/".length())))
                                .findAny()
                                .orElse(-1);

                        // 抛出异常
                        throw new ApiException(status, response.ret());

                    }

                    // 正常结果则消费响应
                    consumer.accept(response);

                    // 合并响应
                    responseRef.updateAndGet(r -> accumulator.apply(r, response));

                }),
                subscriber -> responseRef.get()
        );
    }

    static class Builder<D extends ApiData, R extends ApiResponse<D, R>> implements Buildable<ApiResponseBodyHandler<D, R>, Builder<D, R>> {

        private Function<String, R> deserializer;
        private Consumer<R> consumer;
        private BinaryOperator<R> accumulator;

        public Builder<D, R> deserializer(Function<String, R> deserializer) {
            this.deserializer = requireNonNull(deserializer);
            return this;
        }

        public Builder<D, R> consumer(Consumer<R> consumer) {
            this.consumer = requireNonNull(consumer);
            return this;
        }

        public Builder<D, R> accumulator(BinaryOperator<R> accumulator) {
            this.accumulator = requireNonNull(accumulator);
            return this;
        }

        @Override
        public ApiResponseBodyHandler<D, R> build() {
            return new ApiResponseBodyHandler<>(this);
        }

    }

}
