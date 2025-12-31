package io.github.oldmanpushcart.dashscope4j.client.api;

import io.github.oldmanpushcart.dashscope4j.client.exchange.Exchange;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class ExchangeRequest<M extends AlgoModel, ET, ER> extends AlgoRequest<M, ExchangeResponse> {

    private final Exchange.Handler<ET, ER> handler;

    /**
     * 构造请求
     *
     * @param builder 构建者
     */
    protected ExchangeRequest(Builder<M, ?, ET, ER, ?> builder) {
        super(ExchangeResponse.class, builder);
        this.handler = Objects.requireNonNull(builder.handler, "handler must not be null!");
    }

    @Override
    public BiFunction<HttpResponse<?>, String, ExchangeResponse> responseDecoder() {
        throw new UnsupportedOperationException("unsupported!");
    }

    @Override
    protected Function<ApiRequest<?>, String> requestEncoder() {
        throw new UnsupportedOperationException("unsupported!");
    }

    @Override
    public HttpRequest toHttpRequest(String host) {
        throw new UnsupportedOperationException("unsupported!");
    }

    public abstract Function<ET, String> encoder();

    public abstract Function<String, ER> decoder();

    public Exchange.Handler<ET, ER> handler() {
        return handler;
    }

    public static abstract class Builder<M extends AlgoModel, T extends ExchangeRequest<M, ET, ER>, ET, ER, B extends Builder<M, T, ET, ER, B>>
            extends AlgoRequest.Builder<M, T, B> {

        private Exchange.Handler<ET, ER> handler;

        public B handler(Exchange.Handler<ET, ER> handler) {
            this.handler = Objects.requireNonNull(handler, "handler must not be null!");
            return self();
        }

    }

}
