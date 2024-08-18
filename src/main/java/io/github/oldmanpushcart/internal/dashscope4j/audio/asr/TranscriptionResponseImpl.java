package io.github.oldmanpushcart.internal.dashscope4j.audio.asr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.audio.asr.TranscriptionResponse;
import io.github.oldmanpushcart.dashscope4j.audio.asr.timespan.SentenceTimeSpan;
import io.github.oldmanpushcart.internal.dashscope4j.util.HttpUtils;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import io.github.oldmanpushcart.internal.dashscope4j.util.LazyFetch;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static java.util.Collections.unmodifiableList;

record TranscriptionResponseImpl(String uuid, Ret ret, Usage usage, Output output) implements TranscriptionResponse {

    @JsonCreator
    static TranscriptionResponseImpl of(

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String message,

            @JsonProperty("usage")
            Usage usage,

            @JsonProperty("output")
            OutputImpl output

    ) {
        return new TranscriptionResponseImpl(uuid, Ret.of(code, message), usage, output);
    }

    private record OutputImpl(List<Item> results) implements Output {

        @JsonCreator
        static OutputImpl of(

                @JsonProperty("results")
                List<ItemImpl> results

        ) {
            return new OutputImpl(unmodifiableList(results));
        }

    }

    private static final class ItemImpl implements Item {

        private final Ret ret;
        private final URI originURI;
        private final URI transcriptionURI;
        private final LazyFetch<CompletableFuture<Transcription>> transcriptionFutureLazyFetch = new LazyFetch<>();

        @JsonCreator
        public ItemImpl(

                @JsonProperty("code")
                String code,

                @JsonProperty("message")
                String message,

                @JsonProperty("file_url")
                String originUrl,

                @JsonProperty("transcription_url")
                String transcriptionUrl

        ) {
            this.ret = Ret.of(code, message);
            this.originURI = URI.create(originUrl);
            this.transcriptionURI = URI.create(transcriptionUrl);
        }


        @Override
        public Ret ret() {
            return ret;
        }

        @Override
        public URI originURI() {
            return originURI;
        }

        @Override
        public URI transcriptionURI() {
            return transcriptionURI;
        }

        @Override
        public CompletableFuture<Transcription> lazyFetchTranscription(Executor executor, Duration connectTimeout, Duration timeout) {
            return transcriptionFutureLazyFetch.fetch(() ->
                    HttpUtils.getAsString(transcriptionURI, executor, connectTimeout, timeout)
                            .thenApply(body -> JacksonUtils.toObject(body, TranscriptionImpl.class)));
        }

    }

    private record TranscriptImpl(int channel, Duration duration, String text, List<SentenceTimeSpan> sentences)
            implements Transcript {

        @JsonCreator
        static TranscriptImpl of(

                @JsonProperty("channel_id")
                int channel,

                @JsonProperty("content_duration_in_milliseconds")
                long durationMs,

                @JsonProperty("text")
                String text,

                @JsonProperty("sentences")
                List<SentenceTimeSpan> sentences

        ) {
            return new TranscriptImpl(
                    channel,
                    Duration.ofMillis(durationMs),
                    text,
                    sentences
            );
        }

    }

    private record TranscriptionImpl(URI originURI, Transcription.Meta meta, List<Transcript> transcripts)
            implements TranscriptionResponse.Transcription {

        record MetaImpl(int sampleRate, String format, Duration duration, List<Integer> channels)
                implements Transcription.Meta {

            @JsonCreator
            static MetaImpl of(

                    @JsonProperty("original_sampling_rate")
                    int sampleRate,

                    @JsonProperty("audio_format")
                    String format,

                    @JsonProperty("original_duration_in_milliseconds")
                    long durationMs,

                    @JsonProperty("channels")
                    List<Integer> channels
            ) {
                return new MetaImpl(
                        sampleRate,
                        format,
                        Duration.ofMillis(durationMs),
                        channels
                );
            }

        }

        @JsonCreator
        static TranscriptionImpl of(

                @JsonProperty("file_url")
                String originUrl,

                @JsonProperty("properties")
                MetaImpl meta,

                @JsonProperty("transcripts")
                List<TranscriptImpl> transcripts

        ) {
            return new TranscriptionImpl(
                    URI.create(originUrl),
                    meta,
                    unmodifiableList(transcripts)
            );
        }

    }

}
