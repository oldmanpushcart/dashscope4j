package io.github.oldmanpushcart.dashscope4j.base.upload;

import io.github.oldmanpushcart.dashscope4j.Model;

import java.net.URI;

/**
 * 上传应答
 *
 * @since 1.4.0
 */
public interface UploadResponse {

    /**
     * @return 应答数据
     */
    Output output();

    /**
     * 应答数据
     */
    interface Output {

        /**
         * @return 上传后的资源地址
         */
        URI uploaded();

        /**
         * @return 上传前的资源地址
         */
        URI resource();

        /**
         * @return 指定模型
         */
        Model model();

    }

}
