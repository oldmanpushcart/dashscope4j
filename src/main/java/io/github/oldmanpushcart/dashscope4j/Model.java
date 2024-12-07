package io.github.oldmanpushcart.dashscope4j;

import com.fasterxml.jackson.annotation.JsonValue;

import java.net.URI;

/**
 * 模型
 */
public interface Model {

    /**
     * @return 模型名称
     */
    @JsonValue
    String name();

    /**
     * @return 模型地址
     */
    URI remote();

    /**
     * @return 模型默认参数
     */
    default Option option() {
        return new Option().unmodifiable();
    }

}
