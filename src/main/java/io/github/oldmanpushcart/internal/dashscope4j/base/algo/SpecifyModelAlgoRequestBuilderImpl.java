package io.github.oldmanpushcart.internal.dashscope4j.base.algo;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.base.algo.SpecifyModelAlgoRequest;

public abstract class SpecifyModelAlgoRequestBuilderImpl<M extends Model, T extends SpecifyModelAlgoRequest<M, ?>, B extends SpecifyModelAlgoRequest.Builder<M, T, B>>
        extends AlgoRequestBuilderImpl<M, T, B>
        implements SpecifyModelAlgoRequest.Builder<M, T, B> {

    protected SpecifyModelAlgoRequestBuilderImpl() {
    }

    protected SpecifyModelAlgoRequestBuilderImpl(T request) {
        super(request.model(), request);
    }

}
