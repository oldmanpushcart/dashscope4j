package io.github.oldmanpushcart.internal.dashscope4j.base.algo;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.base.algo.AlgoRequest;

public abstract class HttpAlgoRequestBuilderImpl<M extends Model, T extends AlgoRequest<M>, B extends AlgoRequest.Builder<M, T, B>>
        extends AlgoRequestBuilderImpl<M, T, B> {
}
