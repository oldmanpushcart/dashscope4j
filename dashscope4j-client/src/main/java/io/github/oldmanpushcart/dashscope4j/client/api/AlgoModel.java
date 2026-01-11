package io.github.oldmanpushcart.dashscope4j.client.api;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.Properties;

/**
 * 算法模型
 */
public abstract class AlgoModel {

    private final String name;
    private final String path;
    private final Map<String, String> features;

    protected AlgoModel(String name, String path, Map<String, String> features) {
        this.name = name;
        this.path = path;
        this.features = Map.copyOf(features);
    }

    protected AlgoModel(String name, String path) {
        this(name, path, Map.of());
    }

    @Override
    public String toString() {
        return name;
    }

    @JsonValue
    public String name() {
        return name;
    }

    public String path() {
        return path;
    }

    public Map<String, String> features() {
        return features;
    }

}
