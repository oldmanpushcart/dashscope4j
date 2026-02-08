package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.ParameterSession;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.CodecHandler;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.CommandHandshakeHandler;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.CommandHandshakeHandler.Mode;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;

import java.util.function.Function;

public class SambertSession extends ParameterSession<SambertModel.In, SambertModel.Out> {

    private final SambertModel model;
    private final String text;

    private SambertSession(Builder builder) {
        super(builder);
        this.model = builder.model;
        this.text = builder.text;
    }

    @Override
    public Function<Realtime.Handler<SambertModel.In, SambertModel.Out>, Realtime.Handler<String, String>> provider() {
        return handler -> {
            final var newSession = SambertSession.newBuilder(SambertSession.this)
                    .parameters(new Parameters()
                            .merge(model.parameters())
                            .merge(parameters())
                            .append("text_type", "PlainText"))
                    .build();
            return new CommandHandshakeHandler(
                    Mode.OUT,
                    newSession,
                    new CodecHandler<>(
                            JacksonJsonUtils::toJson,
                            s -> JacksonJsonUtils.toObject(s, SambertModel.Out.class),
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

    @JsonProperty("model")
    @Override
    public SambertModel model() {
        return model;
    }

    @JsonProperty("input")
    Input input() {
        return new Input(text);
    }

    public String text() {
        return text;
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

    public static class Builder extends ParameterSession.Builder<SambertSession, Builder> {

        private final Parameters parameters = new Parameters();
        private String text;
        private SambertModel model;

        public Builder() {

        }

        public Builder(SambertSession session) {
            super(session);
            this.text = session.text;
            this.model = session.model;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder model(SambertModel model) {
            this.model = model;
            return this;
        }

        @Override
        public SambertSession build() {
            return new SambertSession(this);
        }

    }

}
