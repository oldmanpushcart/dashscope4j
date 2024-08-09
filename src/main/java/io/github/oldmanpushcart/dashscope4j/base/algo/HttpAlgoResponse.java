package io.github.oldmanpushcart.dashscope4j.base.algo;

import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiResponse;

public interface HttpAlgoResponse<D extends HttpAlgoResponse.Output> extends HttpApiResponse<D>, AlgoResponse<D> {

    interface Output extends HttpApiResponse.Output, AlgoResponse.Output {

    }

}
