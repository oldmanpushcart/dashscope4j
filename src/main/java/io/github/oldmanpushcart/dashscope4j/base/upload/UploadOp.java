package io.github.oldmanpushcart.dashscope4j.base.upload;

import io.github.oldmanpushcart.dashscope4j.Model;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * 上传操作
 *
 * @since 1.4.2
 */
public interface UploadOp {

    /**
     * 上传资源
     *
     * @param resource 资源URI
     * @param model    模型
     * @return 上传后的URI
     */
    CompletableFuture<URI> upload(URI resource, Model model);

}
