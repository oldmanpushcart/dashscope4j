package io.github.oldmanpushcart.internal.dashscope4j.image.generation;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageModel;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageResponse;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.SpecifyModelAlgoRequestImpl;

import java.time.Duration;

final class GenImageRequestImpl extends SpecifyModelAlgoRequestImpl<GenImageModel, GenImageResponse>
        implements GenImageRequest {

    private final String prompt;
    private final String negative;

    GenImageRequestImpl(GenImageModel model, Option option, Duration timeout, String prompt, String negative) {
        super(model, new Input(prompt, negative), option, timeout, GenImageResponseImpl.class);
        this.prompt = prompt;
        this.negative = negative;
    }

    @Override
    public String prompt() {
        return prompt;
    }

    @Override
    public String negative() {
        return negative;
    }

    private record Input(
            @JsonProperty("prompt") String prompt,
            @JsonProperty("negative_prompt") String negative
    ) {

    }

}
