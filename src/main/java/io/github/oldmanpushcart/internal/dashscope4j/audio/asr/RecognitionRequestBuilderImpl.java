package io.github.oldmanpushcart.internal.dashscope4j.audio.asr;

import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionModel;
import io.github.oldmanpushcart.dashscope4j.audio.asr.RecognitionRequest;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.AlgoRequestBuilderImpl;

public class RecognitionRequestBuilderImpl
        extends AlgoRequestBuilderImpl<RecognitionModel, RecognitionRequest, RecognitionRequest.Builder>
        implements RecognitionRequest.Builder {

    public RecognitionRequestBuilderImpl() {

    }

    public RecognitionRequestBuilderImpl(RecognitionRequest request) {
        super(request);
    }

    @Override
    public RecognitionRequest build() {
        return new RecognitionRequestImpl(model(), option(), timeout());
    }

}
