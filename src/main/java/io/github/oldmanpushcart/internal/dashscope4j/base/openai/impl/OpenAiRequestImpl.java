package io.github.oldmanpushcart.internal.dashscope4j.base.openai.impl;

import io.github.oldmanpushcart.internal.dashscope4j.base.openai.OpenAiRequest;
import io.github.oldmanpushcart.internal.dashscope4j.base.openai.OpenAiResponse;

import java.time.Duration;

public abstract class OpenAiRequestImpl<R extends OpenAiResponse<?>> implements OpenAiRequest<R> {

    private final Duration timeout;

    protected OpenAiRequestImpl(Duration timeout) {
        this.timeout = timeout;
    }

    public Duration timeout() {
        return timeout;
    }

}
