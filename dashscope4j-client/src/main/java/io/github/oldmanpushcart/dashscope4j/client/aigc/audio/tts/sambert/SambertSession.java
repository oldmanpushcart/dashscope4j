package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

public class SambertSession {

    private final SambertModel model;
    private final Parameters parameters;
    private final String text;

    private SambertSession(Builder builder) {
        this.model = builder.model;
        this.parameters = builder.parameters.unmodifiable();
        this.text = builder.text;
    }

    @JsonProperty("task_group")
    String group() {
        return "audio";
    }

    @JsonProperty("task")
    String task() {
        return "tts";
    }

    @JsonProperty("function")
    String function() {
        return "SpeechSynthesizer";
    }

    @JsonProperty("model")
    SambertModel model() {
        return model;
    }

    @JsonProperty("input")
    Input input() {
        return new Input(text);
    }

    public String text() {
        return text;
    }

    @JsonProperty("parameters")
    public Parameters parameters() {
        return parameters;
    }

    private record Input(
            @JsonProperty("text")
            String text
    ) {

    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(SambertSession session) {
        return new Builder(session);
    }

    public static class Builder implements Buildable<SambertSession, Builder> {

        private final Parameters parameters = new Parameters();
        private String text;
        private SambertModel model;

        public Builder() {

        }

        public Builder(SambertSession session) {
            this.parameters.merge(session.parameters);
            this.text = session.text;
            this.model = session.model;
        }

        public Builder parameters(Parameters parameters) {
            this.parameters.clear();
            this.parameters.merge(parameters);
            return this;
        }

        public Builder addParameter(String name, Object value) {
            this.parameters.append(name, value);
            return this;
        }

        public <T, R> Builder addParameter(Parameters.ParameterKey<T, R> parameterKey, T value) {
            this.parameters.append(parameterKey, value);
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        Builder model(SambertModel model) {
            this.model = model;
            return this;
        }

        @Override
        public SambertSession build() {
            return new SambertSession(this);
        }

    }

}
