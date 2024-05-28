package io.github.oldmanpushcart.internal.dashscope4j.base.openai.impl;

import io.github.oldmanpushcart.internal.dashscope4j.base.openai.OpenAiRequest;

import java.time.Duration;

public abstract class OpenAiRequestBuilderImpl<T extends OpenAiRequest<?>, B extends OpenAiRequest.Builder<T, B>>
        implements OpenAiRequest.Builder<T, B> {

    private Duration timeout;

    public B timeout(Duration timeout) {
        this.timeout = timeout;
        return self();
    }

    protected Duration timeout() {
        return timeout;
    }

}
