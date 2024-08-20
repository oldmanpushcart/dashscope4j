package io.github.oldmanpushcart.dashscope4j.base.api;

import java.util.function.Function;

/**
 * 数据交互类Api请求
 *
 * @param <R> 应答类型
 * @since 2.2.0
 */
public interface ExchangeApiRequest<R extends ExchangeApiResponse<?>> extends ApiRequest {

    /**
     * 数据交互请求编码器
     *
     * @param uuid 通道ID
     * @return 请求编码器
     */
    Function<? super ExchangeApiRequest<?>, String> newExchangeRequestEncoder(String uuid);

    /**
     * 数据交互应答解码器
     *
     * @param uuid 通道ID
     * @return 应答解码器
     */
    Function<String, R> newExchangeResponseDecoder(String uuid);

}
