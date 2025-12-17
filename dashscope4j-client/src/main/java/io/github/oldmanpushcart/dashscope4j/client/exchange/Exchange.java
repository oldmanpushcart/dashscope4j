package io.github.oldmanpushcart.dashscope4j.client.exchange;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

/**
 * 数据交换接口
 *
 * @param <T> 发送数据类型
 */
public interface Exchange<T> extends Closeable {


    String id();

    /**
     * @return 是否已关闭
     */
    boolean isClosed();

    /**
     * 关闭数据交换
     *
     * @return 关闭完成的Stage，当关闭完成时，返回结果为{@code null}
     * @throws IllegalStateException 数据交换已关闭
     */
    CompletionStage<Void> closing();

    @Override
    void close();

    /**
     * 发送应用数据
     *
     * @param data 应用数据
     * @return 发送完成的Stage，当发送完成时，返回结果为{@code null}
     */
    CompletionStage<Void> send(T data);

    /**
     * 发送二进制数据
     *
     * @param buffer 二进制数据缓冲区
     * @return 发送完成的Stage，当发送完成时，返回结果为{@code null}
     */
    CompletionStage<Void> send(ByteBuffer buffer);

    /**
     * 连接处理器
     *
     * @param <R> 接收类型
     */
    interface Handler<T, R> {

        /**
         * 处理连接建立
         *
         * @param exchange 数据交换器
         */
        void onOpen(Exchange<T> exchange);

        /**
         * 处理数据接收
         *
         * @param data 数据
         * @return 接收结果
         */
        CompletionStage<Void> onData(R data);

        /**
         * 处理数据接收（二进制）
         *
         * @param buffer 二进制数据
         * @return 接收结果
         */
        CompletionStage<Void> onBinary(ByteBuffer buffer);

        /**
         * 处理数据交换关闭
         *
         * @param ex 导致关闭的异常
         */
        void onClosed(Throwable ex);

    }

}