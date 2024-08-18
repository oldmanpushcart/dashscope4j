package io.github.oldmanpushcart.internal.dashscope4j.audio.asr;

import io.github.oldmanpushcart.dashscope4j.audio.asr.TranscriptionModel;
import io.github.oldmanpushcart.dashscope4j.audio.asr.TranscriptionRequest;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.AlgoRequestBuilderImpl;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils.UpdateMode.REPLACE_ALL;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils.updateList;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils.requireNotEmpty;
import static java.util.Objects.requireNonNull;

public class TranscriptionRequestBuilderImpl
        extends AlgoRequestBuilderImpl<TranscriptionModel, TranscriptionRequest, TranscriptionRequest.Builder>
        implements TranscriptionRequest.Builder {

    private final List<URI> resources = new ArrayList<>();

    public TranscriptionRequestBuilderImpl() {
    }

    public TranscriptionRequestBuilderImpl(TranscriptionRequest request) {
        super(request);
        updateList(REPLACE_ALL, this.resources, request.resources());
    }

    @Override
    public TranscriptionRequest.Builder resources(List<URI> resources) {
        requireNonNull(resources);
        updateList(REPLACE_ALL, this.resources, resources);
        return this;
    }

    @Override
    public TranscriptionRequest build() {
        requireNonNull(model(), "model is required!");
        requireNotEmpty(resources, "sources is empty!");
        return new TranscriptionRequestImpl(
                model(),
                option(),
                timeout(),
                resources
        );
    }

}
