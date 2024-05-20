package io.github.oldmanpushcart.internal.dashscope4j.base.algo;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoResponse;
import io.github.oldmanpushcart.dashscope4j.base.algo.SpecifyModelAlgoRequest;

import java.time.Duration;

public abstract class SpecifyModelAlgoRequestImpl<M extends Model, R extends AlgoResponse<?>>
        extends AlgoRequestImpl<M, R>
        implements SpecifyModelAlgoRequest<M, R> {

    protected SpecifyModelAlgoRequestImpl(M model, Option option, Duration timeout, Class<? extends R> responseType) {
        super(model, option, timeout, responseType);
    }

}
