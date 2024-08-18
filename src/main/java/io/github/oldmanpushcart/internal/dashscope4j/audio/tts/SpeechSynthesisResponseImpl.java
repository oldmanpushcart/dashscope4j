package io.github.oldmanpushcart.internal.dashscope4j.audio.tts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisResponse;
import io.github.oldmanpushcart.dashscope4j.audio.tts.timespan.SentenceTimeSpan;

import java.util.Optional;

public class SpeechSynthesisResponseImpl implements SpeechSynthesisResponse {

    private String uuid;
    private Ret ret;

    private final Output output;
    private final Usage usage;

    private SpeechSynthesisResponseImpl(Output output, Usage usage) {
        this.output = output;
        this.usage = usage;
    }

    @Override
    public String uuid() {
        return uuid;
    }

    void uuid(String uuid) {
        this.uuid = uuid;
    }

    void ret(Ret ret) {
        this.ret = ret;
    }

    @Override
    public Ret ret() {
        return ret;
    }

    @Override
    public Usage usage() {
        return usage;
    }

    @Override
    public Output output() {
        return output;
    }

    record OutputImpl(
            @JsonProperty("sentence")
            SentenceTimeSpan sentence
    ) implements Output {

    }

    @JsonCreator
    static SpeechSynthesisResponseImpl of(

            @JsonProperty("output")
            OutputImpl output,

            @JsonProperty("usage")
            Usage usage

    ) {
        return new SpeechSynthesisResponseImpl(
                output,
                Optional.ofNullable(usage).orElseGet(Usage::empty)
        );
    }

}
