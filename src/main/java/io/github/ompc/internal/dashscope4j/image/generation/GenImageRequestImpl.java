package io.github.ompc.internal.dashscope4j.image.generation;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ompc.dashscope4j.Model;
import io.github.ompc.dashscope4j.Option;
import io.github.ompc.dashscope4j.image.generation.GenImageRequest;
import io.github.ompc.dashscope4j.image.generation.GenImageResponse;
import io.github.ompc.internal.dashscope4j.base.algo.AlgoRequestImpl;

import java.time.Duration;

final class GenImageRequestImpl extends AlgoRequestImpl<GenImageResponse> implements GenImageRequest {

    GenImageRequestImpl(Model model, Input input, Option option, Duration timeout) {
        super(model, input, option, timeout, GenImageResponseImpl.class);
    }

    @Override
    public String toString() {
        return "dashscope://image/generation";
    }

    public record InputImpl(

            @JsonProperty("prompt")
            String prompt,

            @JsonProperty("negative_prompt")
            String negative

    ) implements Input {

    }

}
