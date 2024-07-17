package io.github.oldmanpushcart.internal.dashscope4j.image.generation;

import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageModel;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageRequest;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.AlgoRequestBuilderImpl;

import static java.util.Objects.requireNonNull;

public class GenImageRequestBuilderImpl
        extends AlgoRequestBuilderImpl<GenImageModel, GenImageRequest, GenImageRequest.Builder>
        implements GenImageRequest.Builder {

    private String prompt;
    private String negative;

    public GenImageRequestBuilderImpl() {
    }

    public GenImageRequestBuilderImpl(GenImageRequest request) {
        super(request);
        this.prompt = request.prompt();
        this.negative = request.negative();
    }

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
        requireNonNull(model(), "model is required!");
        return new GenImageRequestImpl(
                model(),
                option(),
                timeout(),
                prompt,
                negative
        );
    }

}
