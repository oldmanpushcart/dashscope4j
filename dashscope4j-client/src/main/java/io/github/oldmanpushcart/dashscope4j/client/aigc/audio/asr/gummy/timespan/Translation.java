package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.gummy.timespan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Translation(

        @JsonProperty("sentence_id")
        int sentenceId,

        @JsonProperty("lang")
        String lang,

        @JsonProperty("begin_time")
        int beginAt,

        @JsonProperty("end_time")
        int endAt,

        @JsonProperty("text")
        String text,

        @JsonProperty("words")
        List<Word> words,

        @JsonProperty("sentence_end")
        boolean sentenceEnd

) {
}
