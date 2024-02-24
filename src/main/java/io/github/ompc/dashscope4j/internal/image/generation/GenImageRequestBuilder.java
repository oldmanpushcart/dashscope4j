package io.github.ompc.dashscope4j.internal.image.generation;

import io.github.ompc.dashscope4j.Option;
import io.github.ompc.dashscope4j.image.generation.GenImageModel;
import io.github.ompc.dashscope4j.image.generation.GenImageRequest;

import java.time.Duration;

import static java.util.Objects.requireNonNull;

public class GenImageRequestBuilder implements GenImageRequest.Builder{

    private String prompt;
    private String negative;
    private GenImageModel model;
    private final Option option = new Option();
    private Duration timeout;

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
    public GenImageRequest.Builder model(GenImageModel model) {
        this.model = requireNonNull(model);
        return this;
    }

    @Override
    public <OT, OR> GenImageRequest.Builder option(Option.Opt<OT, OR> opt, OT value) {
        this.option.option(opt, value);
        return this;
    }

    @Override
    public GenImageRequest.Builder option(String name, Object value) {
        this.option.option(name, value);
        return this;
    }

    @Override
    public GenImageRequest.Builder timeout(Duration timeout) {
        this.timeout = requireNonNull(timeout);
        return this;
    }

    @Override
    public GenImageRequest build() {
        return new GenImageRequestImpl(model, new GenImageRequestImpl.InputImpl(prompt, negative), option, timeout);
    }

}
