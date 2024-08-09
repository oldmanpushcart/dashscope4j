package io.github.oldmanpushcart.internal.dashscope4j.base.openai;

import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiRequest;

/**
 * OpenAi 格式的请求
 *
 * @param <R>
 */
public interface OpenAiRequest<R extends OpenAiResponse<?>> extends HttpApiRequest<R> {

    interface Builder<T extends OpenAiRequest<?>, B extends Builder<T, B>> extends HttpApiRequest.Builder<T, B> {

    }

}
