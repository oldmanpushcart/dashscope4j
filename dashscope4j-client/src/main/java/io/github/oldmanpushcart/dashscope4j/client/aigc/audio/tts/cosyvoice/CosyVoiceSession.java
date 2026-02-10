package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceModel.In;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.CosyVoiceModel.Out;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.HandlerChain;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.ParameterSession;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.CodecHandler;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.CommandHandshakeHandler;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.CommandHandshakeHandler.Mode;

import java.util.HashMap;
import java.util.function.Function;

public class CosyVoiceSession extends ParameterSession<In, Out> {

    private final CosyVoiceModel model;

    public CosyVoiceSession(Builder builder) {
        super(builder);
        this.model = builder.model;
    }

    @Override
    public Function<Realtime.Handler<In, Out>, Realtime.Handler<String, String>> provider() {
        return ioHandler -> {
            final var newSession = CosyVoiceSession.newBuilder(this)
                    .parameters(new Parameters()
                            .merge(model.parameters())
                            .merge(parameters())
                            .append("text_type", "PlainText"))
                    .build();
            return HandlerChain
                    .<String, String>identity()
                    .<String, String>then(h -> new CommandHandshakeHandler(Mode.DUPLEX, newSession, h))
                    .<In, Out>then(h -> CodecHandler.json(In.class, Out.class, h))
                    .filterOutput(o -> o.output().sentence() != null)
                    .mapEmitter(CosyVoiceEmitter::new)
                    .build(ioHandler);
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
    @Override
    public CosyVoiceModel model() {
        return model;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(CosyVoiceSession session) {
        return new Builder(session);
    }

    public static class Builder extends ParameterSession.Builder<CosyVoiceSession, Builder> {

        private CosyVoiceModel model;

        public Builder() {

        }

        public Builder(CosyVoiceSession session) {
            super(session);
            this.model = session.model;
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
