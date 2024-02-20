package io.github.ompc.dashscope4j;

import com.fasterxml.jackson.annotation.JsonValue;

import java.net.URI;

/**
 * 模型
 */
public abstract class Model {

    @JsonValue
    private final String name;
    private final URI remote;

    /**
     * 构造模型
     *
     * @param name   模型名称
     * @param remote 模型地址
     */
    protected Model(String name, URI remote) {
        this.name = name;
        this.remote = remote;
    }

    /**
     * 获取模型名称
     *
     * @return 模型名称
     */
    public String name() {
        return name;
    }

    /**
     * 获取模型地址
     *
     * @return 模型地址
     */
    public URI remote() {
        return remote;
    }

}
