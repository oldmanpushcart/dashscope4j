package io.github.oldmanpushcart.internal.dashscope4j.image.generation;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageResponse;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.AlgoRequestImpl;

import java.time.Duration;

final class GenImageRequestImpl extends AlgoRequestImpl<GenImageResponse> implements GenImageRequest {

    GenImageRequestImpl(Model model, Option option, Duration timeout, String prompt, String negative) {
        super(model, new Input(prompt, negative), option, timeout, GenImageResponseImpl.class);
    }

    private record Input(
            @JsonProperty("prompt")
            String prompt,
            @JsonProperty("negative_prompt")
            String negative
    ) {

    }

}
