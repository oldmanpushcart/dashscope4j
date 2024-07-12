package io.github.oldmanpushcart.internal.dashscope4j.image.generation;

import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageModel;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageResponse;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.AlgoRequestImpl;

import java.time.Duration;
import java.util.HashMap;

final class GenImageRequestImpl extends AlgoRequestImpl<GenImageModel, GenImageResponse>
        implements GenImageRequest {

    private final String prompt;
    private final String negative;

    GenImageRequestImpl(GenImageModel model, Option option, Duration timeout, String prompt, String negative) {
        super(model, option, timeout, GenImageResponseImpl.class);
        this.prompt = prompt;
        this.negative = negative;
    }

    @Override
    public String suite() {
        return "dashscope://image/generation";
    }

    @Override
    public String prompt() {
        return prompt;
    }

    @Override
    public String negative() {
        return negative;
    }

    @Override
    protected Object input() {
        return new HashMap<>() {{
            put("prompt", prompt);
            put("negative_prompt", negative);
        }};
    }

}
