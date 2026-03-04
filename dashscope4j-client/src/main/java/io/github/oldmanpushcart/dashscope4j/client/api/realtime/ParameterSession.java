package io.github.oldmanpushcart.dashscope4j.client.api.realtime;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * 带参数的会话
 * <p>
 * 在部分实时交互的协议中，需要将会话在建立握手时传递给服务器。
 * 这部分会话需要携带参数作为握手的必要信息，所以单独抽出一个抽象类来承载。
 * </p>
 *
 * @param <I> 输入参数类型
 * @param <O> 输出参数类型
 */
public abstract class ParameterSession<I, O> implements Realtime.Session<I, O> {

    private final Map<String, Object> parameters;

    /**
     * 构造会话参数
     *
     * @param builder 构建器
     */
    protected ParameterSession(Builder<?, ?> builder) {
        this.parameters = null != builder.parameters
                ? builder.parameters
                : Map.of();
    }

    /**
     * @return 会话参数
     */
    @JsonProperty("parameters")
    public Map<String, Object> parameters() {
        return parameters;
    }


    /**
     * 参数会话构建器
     *
     * @param <S> 会话类型
     * @param <B> 构建器类型
     */
    public static abstract class Builder<S extends ParameterSession<?, ?>, B extends Builder<S, B>> implements Buildable<S, B> {

        private Map<String, Object> parameters;

        public Builder() {

        }

        public Builder(S session) {
            this.parameters = session.parameters();
        }

        /**
         * 设置参数
         *
         * @param parameters 参数
         * @return this
         */
        public B parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return self();
        }

        public B parameters(UnaryOperator<Map<String, Object>> operator) {
            final var newParameters = null == this.parameters
                    ? new HashMap<String, Object>()
                    : new HashMap<>(this.parameters);
            return parameters(operator.apply(newParameters));
        }

    }

}
