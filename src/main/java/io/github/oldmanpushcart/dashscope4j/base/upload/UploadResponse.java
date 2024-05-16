package io.github.oldmanpushcart.dashscope4j.base.upload;

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
         * 上传后的资源地址
         *
         * @return 资源地址
         */
        URI uploaded();

    }

}
