package io.github.oldmanpushcart.internal.dashscope4j.base.store;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiResponse;

import java.net.URI;

/**
 * 上传数据响应
 */
public record StoreUploadResponse(
        String uuid,
        Ret ret,
        Usage usage,
        Output output
) implements HttpApiResponse<StoreUploadResponse.Output> {

    public record Output(URI resource, Model model, URI uploaded) {
    }

}
