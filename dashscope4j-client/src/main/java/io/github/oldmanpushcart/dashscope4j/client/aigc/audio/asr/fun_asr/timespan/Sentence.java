package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.fun_asr.timespan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Sentence(

        @JsonProperty("begin_time")
        int beginAt,

        @JsonProperty("end_time")
        Integer endAt,

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


}
