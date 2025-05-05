package io.github.oldmanpushcart.dashscope4j.client;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;

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
    Option option();

    @Getter
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    class BaseModel implements Model {

        private final String name;
        private final URI remote;
        private final Option option;

        public BaseModel(String name, URI remote, Option option) {
            this.name = name;
            this.remote = remote;
            this.option = new Option().merge(option).unmodifiable();
        }

        public BaseModel(String name, URI remote) {
            this(name, remote, new Option().unmodifiable());
        }

    }

}
