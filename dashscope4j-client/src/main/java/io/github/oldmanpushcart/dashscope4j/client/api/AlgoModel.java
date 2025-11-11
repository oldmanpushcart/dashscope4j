package io.github.oldmanpushcart.dashscope4j.client.api;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonView;

import java.net.URI;

/**
 * 算法模型
 */
public abstract class AlgoModel {

    private final String name;
    private final URI endpoint;
    private final Parameters parameters;

    /**
     * 构造模型
     *
     * @param name       模型名称
     * @param endpoint   模型服务端点
     * @param parameters 模型参数
     */
    public AlgoModel(String name, URI endpoint, Parameters parameters) {
        this.name = name;
        this.endpoint = endpoint;
        this.parameters = parameters;
    }

    /**
     * 构造模型
     *
     * @param name     模型名称
     * @param endpoint 模型服务端点
     */
    public AlgoModel(String name, URI endpoint) {
        this(name, endpoint, new Parameters());
    }

    /**
     * @return 模型名称
     */
    @JsonValue
    public String name() {
        return name;
    }

    /**
     * @return 模型服务端点
     */
    public URI endpoint() {
        return endpoint;
    }

    /**
     * @return 模型参数
     */
    public Parameters parameters() {
        return parameters;
    }

}
