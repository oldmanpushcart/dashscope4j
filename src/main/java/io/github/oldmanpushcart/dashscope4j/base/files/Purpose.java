package io.github.oldmanpushcart.dashscope4j.base.files;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 文件用途
 */
public enum Purpose {

    /**
     * 文件提取
     */
    @JsonProperty("file-extract")
    FILE_EXTRACT,

    /**
     * 批量提取
     */
    @JsonProperty("batch")
    BATCH,

    /**
     * 批量提取输出
     *
     * @since 3.1.0
     */
    @JsonProperty("batch_output")
    BATCH_OUTPUT,

}
