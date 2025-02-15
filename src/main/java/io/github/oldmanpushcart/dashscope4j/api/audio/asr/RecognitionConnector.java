package io.github.oldmanpushcart.dashscope4j.api.audio.asr;

import io.github.oldmanpushcart.dashscope4j.ExchangeConnector;
import io.github.oldmanpushcart.dashscope4j.OpExchange;
import io.github.oldmanpushcart.dashscope4j.internal.api.audio.asr.RecognitionConnectorImpl;

import java.nio.channels.ReadableByteChannel;

/**
 * 语音识别连接器
 *
 * @since 3.1.0
 */
public interface RecognitionConnector extends ExchangeConnector {

    /**
     * 语音识别结果监听器
     */
    @FunctionalInterface
    interface Listener {

        /**
         * 语音识别结果
         * @param response 语音识别结果
         */
        void onResponse(RecognitionResponse response);

    }

    static Builder newBuilder() {
        return new RecognitionConnectorImpl.Builder();
    }

    /**
     * 语音识别连接器构建器
     */
    interface Builder extends ExchangeConnector.Builder<RecognitionConnector, Builder> {

        /**
         * 设置音频数据通道
         *
         * @param channel 语音识别数据通道
         * @return this
         */
        Builder channel(ReadableByteChannel channel);

        /**
         * 设置音频数据通道缓冲区大小
         *
         * @param bufferSize 缓冲区大小
         * @return this
         */
        Builder channelBufferSize(int bufferSize);

        /**
         * 设置语音识别结果监听器
         *
         * @param listener 语音识别结果监听器
         * @return this
         */
        Builder listener(Listener listener);

        /**
         * 设置语音识别请求
         *
         * @param request 语音识别请求
         * @return this
         */
        Builder request(RecognitionRequest request);

        /**
         * 设置语音识别数据交换操作
         *
         * @param opExchange 语音识别数据交换操作
         * @return this
         */
        Builder opExchange(OpExchange<RecognitionRequest, RecognitionResponse> opExchange);

    }

}
