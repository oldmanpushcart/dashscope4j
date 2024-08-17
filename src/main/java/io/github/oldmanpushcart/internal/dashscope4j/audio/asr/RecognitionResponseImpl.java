package io.github.oldmanpushcart.internal.dashscope4j.audio.asr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionResponse;

import java.util.Optional;

public class RecognitionResponseImpl implements RecognitionResponse {

    private String uuid;
    private Ret ret;

    private final Output output;
    private final Usage usage;

    private RecognitionResponseImpl(Output output, Usage usage) {
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

    @Override
    public Ret ret() {
        return ret;
    }

    void ret(Ret ret) {
        this.ret = ret;
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
    static RecognitionResponseImpl of(

            @JsonProperty("output")
            OutputImpl output,

            @JsonProperty("usage")
            Usage usage

    ) {
        return new RecognitionResponseImpl(
                output,
                Optional.ofNullable(usage).orElseGet(Usage::empty)
        );
    }

}
