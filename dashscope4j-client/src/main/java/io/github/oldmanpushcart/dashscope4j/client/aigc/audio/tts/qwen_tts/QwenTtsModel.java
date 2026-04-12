package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts.internal.interceptor.SettingInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcModel;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Accumulator;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.ByteBufferBase64JsonDeserializer;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.InstantSecondJsonDeserializer;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static io.github.oldmanpushcart.dashscope4j.client.Constants.MULTIMODAL_GENERATION_PATH;

public record QwenTtsModel(String name, String path) implements AigcModel<QwenTtsModel.Input, QwenTtsModel.Output> {

    public static final QwenTtsModel QWEN3_TTS_FLASH = new QwenTtsModel("qwen3-tts-flash");
    public static final QwenTtsModel QWEN_TTS = new QwenTtsModel("qwen-tts");

    private QwenTtsModel(String name) {
        this(name, MULTIMODAL_GENERATION_PATH);
    }

    private static final List<Interceptor> interceptors = List.of(
            new SettingInterceptor()
    );

    @Override
    public List<Interceptor> interceptors() {
        return Stream.of(AigcModel.super.interceptors(), interceptors)
                .flatMap(List::stream)
                .toList();
    }

    public static class Input {

        private final String text;
        private final String voice;
        private final String language;
        private final boolean stream;

        private Input(Builder builder) {
            this.text = builder.text;
            this.voice = builder.voice;
            this.language = builder.language;
            this.stream = builder.stream;
        }

        @JsonProperty("text")
        public String text() {
            return text;
        }

        @JsonProperty("voice")
        public String voice() {
            return voice;
        }

        @JsonProperty("language_type")
        public String language() {
            return language;
        }

        @JsonProperty("stream")
        public boolean stream() {
            return stream;
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public static Builder newBuilder(Input input) {
            return new Builder(input);
        }

        public static class Builder implements Buildable<Input, Builder> {

            private String text;
            private String voice;
            private String language;
            private boolean stream;

            public Builder() {

            }

            public Builder(Input input) {
                this.text = input.text;
                this.voice = input.voice;
                this.language = input.language;
                this.stream = input.stream;
            }

            public Builder text(String text) {
                this.text = text;
                return this;
            }

            public Builder voice(String voice) {
                this.voice = voice;
                return this;
            }

            public Builder language(String language) {
                this.language = language;
                return this;
            }

            public Builder stream(boolean stream) {
                this.stream = stream;
                return this;
            }

            @Override
            public Input build() {
                return new Input(this);
            }

        }

    }

    public record Output(

            @JsonProperty("finish_reason")
            Finish finish,

            @JsonProperty("audio")
            Audio audio

    ) implements Accumulator<Output> {

        @Override
        public Output accumulate(Output output) {
            return new Output(
                    finish,
                    audio.accumulate(output.audio)
            );
        }

        public record Audio(

                @JsonProperty("id")
                String id,

                @JsonProperty("data")
                @JsonDeserialize(using = ByteBufferBase64JsonDeserializer.class)
                ByteBuffer data,

                @JsonProperty("url")
                URI url,

                @JsonProperty("expires_at")
                @JsonDeserialize(using = InstantSecondJsonDeserializer.class)
                Instant expiresAt

        ) implements Accumulator<Audio> {

            @Override
            public Audio accumulate(Audio other) {

                final var limit = data.limit() + other.data.limit();
                final var buffer = ByteBuffer.allocate(limit)
                        .put(data)
                        .put(other.data)
                        .flip();

                return new Audio(
                        id,
                        buffer,
                        url,
                        expiresAt
                );
            }

        }

        public enum Finish {

            @JsonProperty("null")
            NONE,

            @JsonProperty("stop")
            STOP

        }

    }

}
