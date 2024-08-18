package io.github.oldmanpushcart.dashscope4j.base.exchange;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * 数据交互通道
 *
 * @param <T> 流入数据类型
 * @param <R> 流出数据类型
 * @since 2.2.0
 * <p>
 * 该通道可以帮助客户端和服务端完成{@code <T,R>}和{@link ByteBuffer}两类数据的双向异步交互，
 * </p>
 */
public interface Exchange<T, R> {

    /**
     * @return 通道唯一标识
     */
    String uuid();

    /**
     * @return 通道模式
     */
    Exchange.Mode mode();

    /**
     * 写入数据
     *
     * @param data 数据
     * @return 写入操作
     */
    CompletionStage<Exchange<T, R>> write(T data);

    /**
     * 写入ByteBuffer
     *
     * @param buf  ByteBuffer
     * @param last is the last ByteBuffer
     * @return 写入操作
     */
    CompletionStage<Exchange<T, R>> write(ByteBuffer buf, boolean last);

    /**
     * 写入ByteBuffer
     *
     * @param buf ByteBuffer
     * @return 写入操作
     */
    CompletionStage<Exchange<T, R>> write(ByteBuffer buf);

    /**
     * 结束交互
     * <p>
     * 结束交互操作并不是直接发起通道关闭，而是当且仅当{@link Exchange.Mode#DUPLEX}和{@link Exchange.Mode#IN}时，
     * <ul>
     *     <li>STEP-1: 客户端向服务端发起交互结束请求，</li>
     *     <li>STEP-2: 服务端收到后会停止接收后续数据并发送结束确认</li>
     *     <li>STEP-3: 客户端收到服务端结束交互的确认后发起优雅关闭</li>
     *     <li>STEP-4: 通道关闭</li>
     * </ul>
     *
     * </p>
     *
     * @return 结束操作
     */
    CompletionStage<Exchange<T, R>> finishing();

    /**
     * 关闭
     * <p>
     * 客户端会向服务端发送一个关闭请求，服务端响应后才会关闭通道
     * </p>
     *
     * @param status 关闭状态
     * @param reason 关闭原因
     * @return 关闭操作
     */
    CompletionStage<Exchange<T, R>> close(int status, String reason);

    /**
     * 终止
     * <p>
     * 立刻断开和服务端的数据交换连接，不向服务端发送任何请求
     * </p>
     */
    void abort();

    /**
     * 请求数据
     *
     * @param n 请求数据数量
     */
    void request(long n);

    /**
     * 交互监听器
     *
     * @param <T> 流入数据类型
     * @param <R> 流出数据类型
     */
    interface Listener<T, R> {

        /**
         * 通道打开
         *
         * @param exchange 数据交互通道
         */
        default void onOpen(Exchange<T, R> exchange) {
            exchange.request(1L);
        }

        /**
         * 接收数据
         *
         * @param exchange 数据交互通道
         * @param data     数据
         * @return 接收操作
         */
        default CompletionStage<?> onData(Exchange<T, R> exchange, R data) {
            return completedFuture(null).thenAccept(v -> exchange.request(1L));
        }

        /**
         * 接收ByteBuffer
         *
         * @param exchange 数据交互通道
         * @param buf      ByteBuffer
         * @param last     is the last ByteBuffer
         * @return 接收操作
         */
        default CompletionStage<?> onByteBuffer(Exchange<T, R> exchange, ByteBuffer buf, boolean last) {
            return completedFuture(null).thenAccept(v -> exchange.request(1L));
        }

        /**
         * 交互完成
         *
         * @param exchange 数据交互通道
         * @param status   结束状态
         * @param reason   结束原因
         * @return 交互完成
         */
        default CompletionStage<?> onCompleted(Exchange<T, R> exchange, int status, String reason) {
            return completedFuture(null);
        }

        /**
         * 交互失败
         *
         * @param exchange 数据交互通道
         * @param ex       失败异常
         */
        default void onError(Exchange<T, R> exchange, Throwable ex) {

        }

    }

    /**
     * 数据交互模式
     */
    enum Mode {

        /**
         * 普通
         * <p>一次发送，一次接收</p>
         */
        @JsonProperty("none")
        NONE,

        /**
         * 流式输入
         * <p>多次输入，一次输出</p>
         */
        @JsonProperty("in")
        IN,

        /**
         * 流式输出
         * <p>一次输入，多次输出</p>
         */
        @JsonProperty("out")
        OUT,

        /**
         * 双工
         * <p>多次输入，多次输出</p>
         */
        @JsonProperty("duplex")
        DUPLEX

    }

}
