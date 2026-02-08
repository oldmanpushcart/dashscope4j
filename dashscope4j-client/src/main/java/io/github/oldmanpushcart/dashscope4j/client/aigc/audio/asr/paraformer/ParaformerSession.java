package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.paraformer;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.paraformer.ParaformerModel.In;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.paraformer.ParaformerModel.Out;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.HandlerChain;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.ParameterSession;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.CodecHandler;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.CommandHandshakeHandler;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.CommandHandshakeHandler.Mode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

public class ParaformerSession extends ParameterSession<In, Out> {

    private final ParaformerModel model;
    private final List<Resource> resources;

    private ParaformerSession(Builder builder) {
        super(builder);
        this.model = builder.model;
        this.resources = List.copyOf(builder.resources);
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
    public ParaformerModel model() {
        return model;
    }

    @Override
    public Function<Realtime.Handler<In, Out>, Realtime.Handler<String, String>> provider() {
        return ioHandler -> HandlerChain
                .<String, String>identity()
                .<String, String>then(h -> new CommandHandshakeHandler(Mode.DUPLEX, this, h))
                .<In, Out>then(h -> CodecHandler.json(In.class, Out.class, h))
                .filterOutput(o -> o.output().sentence() != null)
                .build(ioHandler);
    }

    @JsonProperty("resources")
    public List<Resource> resources() {
        return resources;
    }

    public record Resource(

            @JsonProperty("resource_id")
            String id

    ) {

        @JsonProperty("resource_type")
        String type() {
            return "asr_phrase";
        }

    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(ParaformerSession session) {
        return new Builder(session);
    }

    public static class Builder extends ParameterSession.Builder<ParaformerSession, Builder> {

        private ParaformerModel model;
        private final Parameters parameters = new Parameters();
        private final List<Resource> resources = new ArrayList<>();

        public Builder() {
        }

        public Builder(ParaformerSession session) {
            super(session);
            this.model = session.model;
            this.resources.addAll(session.resources);
        }

        public Builder model(ParaformerModel model) {
            this.model = model;
            return this;
        }

        public Builder resources(List<Resource> resources) {
            this.resources.clear();
            this.resources.addAll(resources);
            return this;
        }

        public Builder addResources(List<Resource> resources) {
            this.resources.addAll(resources);
            return this;
        }

        public Builder addResource(Resource resource) {
            this.resources.add(resource);
            return this;
        }

        @Override
        public ParaformerSession build() {
            return new ParaformerSession(this);
        }

    }

}
