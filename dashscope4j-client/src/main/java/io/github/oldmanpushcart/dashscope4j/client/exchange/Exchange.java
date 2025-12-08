package io.github.oldmanpushcart.dashscope4j.client.exchange;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 数据交换接口
 *
 * @param <T> 发送数据类型
 * @param <R> 接收数据类型
 */
public interface Exchange<T, R> extends Closeable {

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

    interface Codec<T, R> {

        String encode(T t);

        R decode(String s);

        Codec<String, String> identity = new Codec<>() {

            @Override
            public String encode(String s) {
                return s;
            }

            @Override
            public String decode(String s) {
                return s;
            }

        };

    }

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
            return CompletableFuture.completedStage(null);
        }

        @Override
        public CompletionStage<Void> onBinary(ByteBuffer buffer) {
            return CompletableFuture.completedStage(null);
        }

        @Override
        public CompletionStage<Void> onClosed(Throwable ex) {
            return CompletableFuture.completedStage(null);
        }

    }

}