package io.github.oldmanpushcart.dashscope4j.base.files;

import java.net.URI;

/**
 * 文件元数据
 *
 * @since 1.4.2
 */
public interface FileMeta {

    /**
     * @return 文件ID
     */
    String id();

    /**
     * @return 文件名
     */
    String name();

    /**
     * @return 文件大小
     */
    long size();

    /**
     * @return 文件上传时间
     */
    long uploadedAt();

    /**
     * @return 文件用途
     */
    String purpose();

    /**
     * @return 转换为URI
     */
    URI toURI();

}
