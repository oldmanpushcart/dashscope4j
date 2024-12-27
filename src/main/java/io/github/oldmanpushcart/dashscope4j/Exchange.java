package io.github.oldmanpushcart.dashscope4j;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;

/**
 * 数据交换
 *
 * @param <T> 流入数据类型
 *            <p>
 *            可以帮助客户端和服务端完成{@code <T,R>}和{@link ByteBuffer}两类数据的双向异步交换
 *            </p>
 */
public interface Exchange<T> {

    /**
     * 正常关闭
     */
    int NORMAL_CLOSURE = 1000;

    /**
     * 内部错误
     * <p>遇到未预期的状态或错误</p>
     */
    int INTERNAL_ERROR_CLOSURE = 1011;

    /**
     * @return 唯一标识
     */
    String uuid();

    /**
     * @return 交换模式
     */
    Mode mode();

    /**
     * 写入数据
     *
     * @param data 数据
     * @return 是否成功
     */
    boolean write(T data);

    /**
     * 写入ByteBuffer
     *
     * @param buf ByteBuffer
     * @return 是否成功
     */
    boolean write(ByteBuffer buf);

    /**
     * 申请结束
     * <p>
     * 结束操作并不是直接发起数据交换连接关闭，而是当且仅当{@link Mode#DUPLEX}和{@link Mode#IN}时，
     * <ul>
     *     <li>STEP-1: 客户端向服务端发起结束请求，</li>
     *     <li>STEP-2: 服务端收到后会停止接收后续数据并发送结束确认</li>
     *     <li>STEP-3: 客户端收到服务端结束的确认后发起优雅关闭</li>
     *     <li>STEP-4: 交换关闭</li>
     * </ul>
     * </p>
     *
     * @return 是否成功
     */
    boolean finishing();

    /**
     * 申请关闭
     * <p>
     * 客户端会向服务端发送一个关闭请求，服务端响应后才会关闭交换连接
     * </p>
     *
     * @param status 关闭状态
     * @param reason 关闭原因
     * @return 是否成功
     */
    boolean closing(int status, String reason);

    /**
     * 申请异常关闭
     *
     * @param ex 导致关闭的异常
     * @return 是否成功
     */
    boolean closing(Throwable ex);

    /**
     * 申请关闭
     *
     * @return 是否成功
     */
    boolean closing();

    /**
     * 终止
     * <p>
     * 立刻断开和服务端的数据交换连接，不向服务端发送任何请求
     * </p>
     */
    void abort();

    /**
     * @return 是否已关闭
     */
    boolean isClosed();

    /**
     * @return 关闭通知
     */
    CompletionStage<?> closeStage();

    /**
     * 交换监听器
     *
     * @param <T> 流入数据类型
     * @param <R> 流出数据类型
     */
    interface Listener<T, R> {

        /**
         * 交换打开
         *
         * @param exchange 数据交互通道
         */
        default void onOpen(Exchange<T> exchange) {

        }

        /**
         * 接收数据
         *
         * @param data 数据
         */
        default void onData(R data) {

        }

        /**
         * 接收ByteBuffer
         *
         * @param buf ByteBuffer
         */
        default void onByteBuffer(ByteBuffer buf) {

        }

        /**
         * 交换完成
         */
        default void onCompleted() {

        }

        /**
         * 交换失败
         *
         * @param ex 失败异常
         */
        default void onError(Throwable ex) {

        }

    }

    /**
     * 数据交换模式
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
