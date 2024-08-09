package io.github.oldmanpushcart.dashscope4j.base.api;

public interface HttpApiResponse<D extends HttpApiResponse.Output> extends ApiResponse<D> {

    interface Output extends ApiResponse.Output {

    }

}
