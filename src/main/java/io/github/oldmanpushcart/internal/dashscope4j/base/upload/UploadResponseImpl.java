package io.github.oldmanpushcart.internal.dashscope4j.base.upload;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.base.upload.UploadResponse;

import java.net.URI;

/**
 * 上传响应
 */
public record UploadResponseImpl(Output output) implements UploadResponse {

    public record OutputImpl(
            URI resource,
            Model model,
            URI uploaded
    ) implements UploadResponse.Output {
    }

}
