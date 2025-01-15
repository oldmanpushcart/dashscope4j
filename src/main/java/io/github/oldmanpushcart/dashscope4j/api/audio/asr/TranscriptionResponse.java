package io.github.oldmanpushcart.dashscope4j.api.audio.asr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.api.AlgoResponse;
import io.github.oldmanpushcart.dashscope4j.api.audio.asr.timespan.SentenceTimeSpan;
import io.github.oldmanpushcart.dashscope4j.internal.util.JacksonJsonUtils;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static java.util.Collections.unmodifiableList;

/**
 * 语音转录应答
 */
@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TranscriptionResponse extends AlgoResponse<TranscriptionResponse.Output> {

    Output output;

    @JsonCreator
    private TranscriptionResponse(

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String desc,

            @JsonProperty("usage")
            Usage usage,

            @JsonProperty("output")
            Output output

    ) {
        super(uuid, code, desc, usage);
        this.output = output;
    }

    @Value
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    public static class Output {

        List<Item> results;

        @JsonCreator
        public Output(

                @JsonProperty("results")
                List<Item> results

        ) {
            this.results = unmodifiableList(results);
        }

    }

    @Value
    @Accessors(fluent = true)
    @ToString(callSuper = true)
    @EqualsAndHashCode(callSuper = true)
    public static class Item extends Ret {

        URI originURI;
        URI transcriptionURI;

        @JsonCreator
        private Item(

                @JsonProperty("code")
                String code,

                @JsonProperty("message")
                String desc,

                @JsonProperty("file_url")
                String originUrl,

                @JsonProperty("transcription_url")
                String transcriptionUrl

        ) {
            super(code, desc);
            this.originURI = URI.create(originUrl);
            this.transcriptionURI = URI.create(transcriptionUrl);
        }

        private Transcription decode(String json) {
            return JacksonJsonUtils.toObject(json, Transcription.class);
        }

        public CompletionStage<Transcription> fetchTranscription(Function<URI, CompletionStage<String>> downloader) {
            return downloader.apply(transcriptionURI)
                    .thenApply(this::decode);
        }

    }

    @Value
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    public static class Transcription {

        URI originURI;
        Meta meta;
        List<Transcript> transcripts;

        @JsonCreator
        private Transcription(

                @JsonProperty("file_url")
                String originUrl,

                @JsonProperty("properties")
                Meta meta,

                @JsonProperty("transcripts")
                List<Transcript> transcripts

        ) {
            this.originURI = URI.create(originUrl);
            this.meta = meta;
            this.transcripts = unmodifiableList(transcripts);
        }

    }

    @Value
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    public static class Meta {

        int sampleRate;
        String format;
        Duration duration;
        List<Integer> channels;

        private Meta(

                @JsonProperty("original_sampling_rate")
                int sampleRate,

                @JsonProperty("audio_format")
                String format,

                @JsonProperty("original_duration_in_milliseconds")
                long durationMs,

                @JsonProperty("channels")
                List<Integer> channels

        ) {
            this.sampleRate = sampleRate;
            this.format = format;
            this.duration = Duration.ofMillis(durationMs);
            this.channels = unmodifiableList(channels);
        }

    }

    @Value
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    public static class Transcript {

        int channel;
        Duration duration;
        String text;
        List<SentenceTimeSpan> sentences;

        @JsonCreator
        private Transcript(

                @JsonProperty("channel_id")
                int channel,

                @JsonProperty("text")
                String text,

                @JsonProperty("content_duration_in_milliseconds")
                long durationMs,

                @JsonProperty("sentences")
                List<SentenceTimeSpan> sentences

        ) {
            this.channel = channel;
            this.text = text;
            this.duration = Duration.ofMillis(durationMs);
            this.sentences = unmodifiableList(sentences);
        }

    }

}
