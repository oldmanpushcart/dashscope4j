package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.sambert.timespan.SentenceTimeSpan;
import io.github.oldmanpushcart.dashscope4j.client.api.Model;
import io.github.oldmanpushcart.dashscope4j.client.api.Parameters;
import io.github.oldmanpushcart.dashscope4j.client.api.Ret;
import io.github.oldmanpushcart.dashscope4j.client.api.Usage;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.RealtimeModel;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.CodecHandler;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.CommandHandler;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;

import java.util.function.BiFunction;

public record SambertModel(
        String name,
        String path,
        Parameters parameters
) implements RealtimeModel<SambertSession, SambertModel.In, SambertModel.Out> {

    public static final String PATH = "/api-ws/v1/inference/";

    public static final SambertModel ZHINAN = new SambertModel(
            "sambert-zhinan-v1",
            new Parameters()
                    .append("sample_rate", 48000)
    );

    public SambertModel(String name, Parameters parameters) {
        this(name, PATH, parameters);
    }

    public SambertModel(String name, String path, Parameters parameters) {
        this.name = name;
        this.path = path;
        this.parameters = parameters.unmodifiable();
    }

    @Override
    public BiFunction<SambertSession, Realtime.Handler<In, Out>, Realtime.Handler<String, String>> provider() {
        return (session, handler) -> {
            final var newSession = SambertSession.newBuilder(session)
                    .model(this)
                    .parameters(new Parameters()
                            .merge(parameters)
                            .merge(session.parameters())
                            .append("text_type", "PlainText"))
                    .build();
            return new CommandHandler(
                    CommandHandler.Mode.OUT,
                    newSession,
                    new CodecHandler<>(
                            JacksonJsonUtils::toJson,
                            s -> JacksonJsonUtils.toObject(s, Out.class),
                            handler
                    )
            );
        };
    }

    public static class In {

        private In() {

        }

    }

    public record Out(

            @JsonProperty("output")
            Output output,

            @JsonProperty
            Usage usage

    ) {

        public record Output(

                @JsonProperty("sentence")
                SentenceTimeSpan sentence

        ) {

        }

    }


}
