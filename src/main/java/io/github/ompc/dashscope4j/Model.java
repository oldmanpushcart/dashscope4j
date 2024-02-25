package io.github.ompc.dashscope4j;

import com.fasterxml.jackson.annotation.JsonValue;

import java.net.URI;

/**
 * 模型
 */
public interface Model {

    /**
     * 获取模型名称
     *
     * @return 模型名称
     */
    @JsonValue
    String name();

    /**
     * 获取模型地址
     *
     * @return 模型地址
     */
    URI remote();

}
