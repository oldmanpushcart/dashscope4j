package io.github.oldmanpushcart.dashscope4j.client.api.realtime;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.util.Map;

/**
 * 参数会话
 *
 * @param <I> 输入参数类型
 * @param <O> 输出参数类型
 */
public abstract class ParameterSession<I, O> implements Realtime.Session<I, O> {

    private final Map<String, Object> parameters;

    protected ParameterSession(Builder<?, ?> builder) {
        this.parameters = null != builder.parameters
                ? builder.parameters
                : Map.of();
    }

    @JsonProperty("parameters")
    public Map<String, Object> parameters() {
        return parameters;
    }


    /**
     *
     * @param <S>
     * @param <B>
     */
    public static abstract class Builder<S extends ParameterSession<?, ?>, B extends Builder<S, B>> implements Buildable<S, B> {

        private Map<String, Object> parameters;

        public Builder() {

        }

        public Builder(S session) {
            this.parameters = session.parameters();
        }

        public B parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return self();
        }

    }

}
