package io.github.oldmanpushcart.dashscope4j.client.exchange;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

/**
 * 数据交换接口
 *
 * @param <T> 发送数据类型
 * @param <R> 接收数据类型
 */
public interface Exchange<T, R> {

    /**
     * 打开数据交换
     *
     * @param handler 数据交换处理器
     * @return 完成时返回已就绪的 {@code Exchange} 实例
     * @throws IllegalStateException 数据交换已打开
     */
    CompletionStage<Exchange<T, R>> open(Handler<T, R> handler);

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
    CompletionStage<Void> close();

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
     * @param <T> 发送数据类型
     * @param <R> 接收数据类型
     */
    interface Handler<T, R> {

        void onOpen(Exchange<T, R> exchange);

        CompletionStage<Void> onData(R data);

        CompletionStage<Void> onBinary(ByteBuffer buffer);

        CompletionStage<Void> onClosed(Throwable ex);

    }

    /**
     * {@link Handler} 的空实现适配器，便于选择性重写回调方法。
     *
     * @param <T> 发送数据类型
     * @param <R> 接收数据类型
     */
    abstract class HandlerAdapter<T, R> implements Handler<T, R> {

        @Override
        public void onOpen(Exchange<T, R> exchange) {

        }

        @Override
        public CompletionStage<Void> onData(R data) {
            return null;
        }

        @Override
        public CompletionStage<Void> onBinary(ByteBuffer buffer) {
            return null;
        }

        @Override
        public CompletionStage<Void> onClosed(Throwable ex) {
            return null;
        }

    }

}