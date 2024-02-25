package io.github.ompc.internal.dashscope4j.image.generation;

import io.github.ompc.dashscope4j.image.generation.GenImageModel;
import io.github.ompc.dashscope4j.image.generation.GenImageRequest;
import io.github.ompc.internal.dashscope4j.algo.AlgoRequestBuilderImpl;

import static java.util.Objects.requireNonNull;

public class GenImageRequestBuilder
        extends AlgoRequestBuilderImpl<GenImageModel, GenImageRequest, GenImageRequest.Builder>
        implements GenImageRequest.Builder {

    private String prompt;
    private String negative;

    @Override
    public GenImageRequest.Builder prompt(String prompt) {
        this.prompt = requireNonNull(prompt);
        return this;
    }

    @Override
    public GenImageRequest.Builder negative(String negative) {
        this.negative = requireNonNull(negative);
        return this;
    }

    @Override
    public GenImageRequest build() {
        return new GenImageRequestImpl(
                requireNonNull(model()),
                new GenImageRequestImpl.InputImpl(prompt, negative),
                option(),
                timeout()
        );
    }

}
