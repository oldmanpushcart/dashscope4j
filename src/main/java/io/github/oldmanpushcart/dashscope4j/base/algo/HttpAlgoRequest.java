package io.github.oldmanpushcart.dashscope4j.base.algo;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiRequest;

public interface HttpAlgoRequest<M extends Model, R extends HttpAlgoResponse<?>> extends HttpApiRequest<R>, AlgoRequest<M> {

    interface Builder<M extends Model, T extends HttpAlgoRequest<M, ?>, B extends Builder<M, T, B>>
            extends HttpApiRequest.Builder<T, B>, AlgoRequest.Builder<M, T, B> {

    }

}
