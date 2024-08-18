package io.github.oldmanpushcart.internal.dashscope4j.audio.asr;

import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.audio.asr.TranscriptionModel;
import io.github.oldmanpushcart.dashscope4j.audio.asr.TranscriptionRequest;
import io.github.oldmanpushcart.dashscope4j.audio.asr.TranscriptionResponse;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.HttpAlgoRequestImpl;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;

public class TranscriptionRequestImpl
        extends HttpAlgoRequestImpl<TranscriptionModel, TranscriptionResponse>
        implements TranscriptionRequest {

    private final List<URI> resources;

    protected TranscriptionRequestImpl(TranscriptionModel model, Option option, Duration timeout, List<URI> resources) {
        super(model, option, timeout, TranscriptionResponseImpl.class);
        this.resources = resources;
    }

    @Override
    public String suite() {
        return "dashscope://audio/asr";
    }

    @Override
    public List<URI> resources() {
        return resources;
    }

    @Override
    protected Object input() {
        return new HashMap<>() {{
            put("file_urls", resources);
        }};
    }

}
