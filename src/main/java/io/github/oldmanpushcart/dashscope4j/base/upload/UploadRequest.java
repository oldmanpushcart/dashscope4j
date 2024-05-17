package io.github.oldmanpushcart.dashscope4j.base.upload;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.util.Buildable;
import io.github.oldmanpushcart.internal.dashscope4j.base.upload.UploadRequestBuilderImpl;

import java.net.URI;
import java.time.Duration;

/**
 * 上传请求
 *
 * @since 1.4.0
 */
public interface UploadRequest extends ApiRequest<UploadResponse> {

    /**
     * @return 指定模型
     */
    Model model();

    /**
     * @return 资源
     */
    URI resource();

    /**
     * @return 请求超时
     */
    Duration timeout();

    /**
     * @return 构造器
     */
    static Builder newBuilder() {
        return new UploadRequestBuilderImpl();
    }

    /**
     * 构造器
     */
    interface Builder extends Buildable<UploadRequest, Builder> {

        /**
         * 设置资源
         *
         * @param resource 资源
         * @return this
         */
        Builder resource(URI resource);

        /**
         * 指定模型
         *
         * @param model 指定模型
         * @return this
         */
        Builder model(Model model);

        /**
         * 设置请求超时
         *
         * @param timeout 请求超时
         * @return this
         */
        Builder timeout(Duration timeout);

    }

}
