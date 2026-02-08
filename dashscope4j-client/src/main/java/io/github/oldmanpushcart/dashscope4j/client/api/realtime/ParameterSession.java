package io.github.oldmanpushcart.dashscope4j.client.api.realtime;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

public abstract class ParameterSession<I, O> implements Realtime.Session<I, O> {

    private final Parameters parameters = new Parameters();

    protected ParameterSession() {
    }

    protected ParameterSession(Builder<?, ?> builder) {
        this.parameters.merge(builder.parameters);
    }

    @JsonProperty("parameters")
    public Parameters parameters() {
        return parameters;
    }

    public static abstract class Builder<S extends ParameterSession<?, ?>, B extends Builder<S, B>> implements Buildable<S, B> {

        private final Parameters parameters = new Parameters();

        public Builder() {

        }

        public Builder(S session) {
            this.parameters.merge(session.parameters());
        }

        public B parameters(Parameters parameters) {
            this.parameters.clear();
            this.parameters.merge(parameters);
            return self();
        }

        public <T, R> B addParameter(Parameters.ParameterKey<T, R> parameterKey, T value) {
            this.parameters.append(parameterKey, value);
            return self();
        }

        public B addParameter(String name, Object value) {
            this.parameters.append(name, value);
            return self();
        }

    }

}
