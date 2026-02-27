package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.cosyvoice.timespan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Sentence(

        @JsonProperty("index")
        int index,

        @JsonProperty("words")
        List<Word> words

) {

}
