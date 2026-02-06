package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.paraformer.timespan;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Sentence(
        Range range,
        Emo emo,
        String text,
        List<Word> words,
        boolean heartbeat,
        boolean end
) {

    @JsonCreator
    private Sentence(

            @JsonProperty("begin_time")
            int beginTime,

            @JsonProperty("end_time")
            Integer endTime,

            @JsonProperty("text")
            String text,

            @JsonProperty("words")
            List<Word> words,

            @JsonProperty("emo_tag")
            String emoTag,

            @JsonProperty("emo_confidence")
            Float emoConfidence,

            @JsonProperty("heartbeat")
            boolean heartbeat,

            @JsonProperty("sentence_end")
            boolean end

    ) {

        this(
                Range.of(beginTime, endTime),
                Emo.of(emoTag, emoConfidence),
                text,
                words,
                heartbeat,
                end
        );

    }

}
