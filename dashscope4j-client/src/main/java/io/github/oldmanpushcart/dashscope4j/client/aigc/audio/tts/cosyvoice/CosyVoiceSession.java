package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.CodecHandler;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.CommandHandler;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;

import java.util.HashMap;
import java.util.function.Function;

public class CosyVoiceSession implements Realtime.Session<CosyVoiceModel.In, CosyVoiceModel.Out> {

    private final CosyVoiceModel model;
    private final Parameters parameters;

    public CosyVoiceSession(Builder builder) {
        this.model = builder.model;
        this.parameters = builder.parameters.unmodifiable();
    }

    @Override
    public Function<Realtime.Handler<CosyVoiceModel.In, CosyVoiceModel.Out>, Realtime.Handler<String, String>> provider() {
        return  handler -> {
            final var newSession = CosyVoiceSession.newBuilder(this)
                    .parameters(new Parameters()
                            .merge(model.parameters())
                            .merge(parameters)
                            .append("text_type", "PlainText"))
                    .build();
            return new CommandHandler(
                    CommandHandler.Mode.DUPLEX,
                    newSession,
                    new CodecHandler<>(
                            JacksonJsonUtils::toJson,
                            s -> JacksonJsonUtils.toObject(s, CosyVoiceModel.Out.class),
                            handler
                    )
            );
        };
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

    @JsonProperty("input")
    Object input() {
        return new HashMap<>();
    }

    @JsonProperty("model")
    public CosyVoiceModel model() {
        return model;
    }

    @JsonProperty("parameters")
    public Parameters parameters() {
        return parameters;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(CosyVoiceSession session) {
        return new Builder(session);
    }

    public static class Builder implements Buildable<CosyVoiceSession, Builder> {

        private final Parameters parameters = new Parameters();
        private CosyVoiceModel model;

        public Builder() {

        }

        public Builder(CosyVoiceSession session) {
            this.model = session.model;
            this.parameters.merge(session.parameters);
        }

        public Builder parameters(Parameters parameters) {
            this.parameters.clear();
            this.parameters.merge(parameters);
            return this;
        }

        public <T, R> Builder addParameter(Parameters.ParameterKey<T, R> parameterKey, T value) {
            this.parameters.append(parameterKey, value);
            return this;
        }

        public <T> Builder addParameter(String name, T value) {
            this.parameters.append(name, value);
            return this;
        }

        public Builder model(CosyVoiceModel model) {
            this.model = model;
            return this;
        }

        @Override
        public CosyVoiceSession build() {
            return new CosyVoiceSession(this);
        }

    }

}
