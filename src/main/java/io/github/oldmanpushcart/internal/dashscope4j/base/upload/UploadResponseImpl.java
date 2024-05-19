package io.github.oldmanpushcart.internal.dashscope4j.base.upload;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.base.upload.UploadResponse;

import java.net.URI;

/**
 * 上传响应
 */
public record UploadResponseImpl(String uuid, Ret ret, Usage usage, Output output) implements UploadResponse {

    public record OutputImpl(
            URI resource,
            Model model,
            URI uploaded
    ) implements UploadResponse.Output {
    }

}
