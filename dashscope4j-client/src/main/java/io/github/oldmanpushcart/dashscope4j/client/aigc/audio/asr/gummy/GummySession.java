package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.gummy;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.gummy.GummyModel.In;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.gummy.GummyModel.Out;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.HandlerChain;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.ParameterSession;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.CodecHandler;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.CommandHandshakeHandler;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.CommandHandshakeHandler.Mode;

import java.util.HashMap;
import java.util.function.Function;

public class GummySession extends ParameterSession<In, Out> {

    private final GummyModel model;

    private GummySession(Builder builder) {
        super(builder);
        this.model = builder.model;
    }

    @JsonProperty("task_group")
    String group() {
        return "audio";
    }

    @JsonProperty("task")
    String task() {
        return "asr";
    }

    @JsonProperty("function")
    String function() {
        return "recognition";
    }

    @JsonProperty("input")
    Object input() {
        return new HashMap<>();
    }

    @JsonProperty("model")
    @Override
    public GummyModel model() {
        return model;
    }

    @Override
    public Function<Realtime.Handler<In, Out>, Realtime.Handler<String, String>> provider() {
        return ioHandler -> HandlerChain
                .<String, String>identity()
                .<String, String>then(h -> new CommandHandshakeHandler(Mode.DUPLEX, this, h))
                .<In, Out>then(h -> CodecHandler.json(In.class, Out.class, h))
                .filterOutput(o -> o.output().transcription() != null)
                .build(ioHandler);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(GummySession session) {
        return new Builder(session);
    }

    public static class Builder extends ParameterSession.Builder<GummySession, Builder> {

        private GummyModel model;

        public Builder() {

        }

        public Builder(GummySession session) {
            super(session);
            this.model = session.model;
        }

        public Builder model(GummyModel model) {
            this.model = model;
            return this;
        }

        @Override
        public GummySession build() {
            return new GummySession(this);
        }

    }

}
