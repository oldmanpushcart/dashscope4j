package io.github.oldmanpushcart.dashscope4j.client;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Set;

/**
 * 算法模型
 */
public abstract class AlgoModel {

    private final String name;
    private final String path;
    private final Set<String> tags;

    protected AlgoModel(String name, String path, Set<String> tags) {
        this.name = name;
        this.path = path;
        this.tags = Set.copyOf(tags);
    }

    protected AlgoModel(String name, String path) {
        this(name, path, Set.of());
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

    public Set<String> tags() {
        return tags;
    }

}
