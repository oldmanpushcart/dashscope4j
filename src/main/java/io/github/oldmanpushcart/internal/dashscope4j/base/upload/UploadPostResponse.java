package io.github.oldmanpushcart.internal.dashscope4j.base.upload;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;

import java.net.URI;

/**
 * 上传请求响应
 */
public record UploadPostResponse(String uuid, Ret ret, Usage usage, Output output)
        implements ApiResponse<UploadPostResponse.Output> {

    public record Output(URI resource, Model model, URI uploaded) implements ApiResponse.Output {
    }

}
