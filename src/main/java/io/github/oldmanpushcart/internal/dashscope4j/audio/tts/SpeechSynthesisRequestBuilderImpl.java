package io.github.oldmanpushcart.internal.dashscope4j.audio.tts;

import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisModel;
import io.github.oldmanpushcart.dashscope4j.audio.tts.SpeechSynthesisRequest;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.AlgoRequestBuilderImpl;

public class SpeechSynthesisRequestBuilderImpl
        extends AlgoRequestBuilderImpl<SpeechSynthesisModel, SpeechSynthesisRequest, SpeechSynthesisRequest.Builder>
        implements SpeechSynthesisRequest.Builder {

    private String text;

    public SpeechSynthesisRequestBuilderImpl() {

    }

    public SpeechSynthesisRequestBuilderImpl(SpeechSynthesisRequest request) {
        super(request);
        this.text = request.text();
    }

    @Override
    public SpeechSynthesisRequest.Builder text(String text) {
        this.text = text;
        return this;
    }

    @Override
    public SpeechSynthesisRequest build() {
        return new SpeechSynthesisRequestImpl(model(), option(), timeout(), text);
    }

}
