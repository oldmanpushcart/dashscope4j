package io.github.oldmanpushcart.internal.dashscope4j.base.algo;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.base.algo.ExchangeAlgoRequest;
import io.github.oldmanpushcart.dashscope4j.base.algo.ExchangeAlgoResponse;

import java.time.Duration;

public abstract class ExchangeAlgoRequestImpl<M extends Model, R extends ExchangeAlgoResponse<?>>
        extends AlgoRequestImpl<M>
        implements ExchangeAlgoRequest<M, R> {

    private final Class<? extends R> responseType;

    protected ExchangeAlgoRequestImpl(M model, Option option, Duration timeout, Class<? extends R> responseType) {
        super(model, option, timeout);
        this.responseType = responseType;
    }

    protected Class<? extends R> responseType() {
        return responseType;
    }



}
