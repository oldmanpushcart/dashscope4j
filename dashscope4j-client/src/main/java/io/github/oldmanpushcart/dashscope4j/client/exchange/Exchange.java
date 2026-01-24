package io.github.oldmanpushcart.dashscope4j.client.exchange;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * 数据交换接口，用于在客户端与服务端之间异步发送应用数据或二进制数据。
 * 该接口支持优雅关闭，并提供关闭完成的异步通知机制。
 *
 * <p>实现类应保证线程安全，允许多个线程并发调用 {@code send} 方法。
 * 所有返回 {@link CompletionStage} 的方法均表示异步操作，其完成可能成功或失败。
 *
 * @param <T> 应用层发送的数据类型（如 JSON 对象、自定义消息等）
 */
public interface Exchange<T> extends Closeable {

    /**
     * 获取当前数据交换的唯一标识符。
     *
     * @return 数据交换的 ID，通常用于日志追踪、会话管理等
     */
    String id();

    /**
     * 判断当前数据交换是否已关闭。
     *
     * <p>一旦返回 {@code true}，后续调用 {@link #send(Object)} 或 {@link #send(ByteBuffer)}
     * 将抛出 {@link IllegalStateException}。
     *
     * @return {@code true} 表示已关闭，{@code false} 表示仍处于活跃状态
     */
    boolean isClosed();

    /**
     * 发起关闭流程并等待服务端响应，但不阻塞当前线程。
     *
     * <p>该方法是幂等的：多次调用不会产生副作用。
     * 若数据交换已关闭，则抛出 {@link IllegalStateException}。
     *
     * @return 表示关闭操作完成的 {@link CompletionStage}；
     * 成功完成时结果为 {@code null}，失败时包含异常原因
     * @throws IllegalStateException 如果数据交换已经关闭
     */
    CompletionStage<Void> closing();

    /**
     * 立即关闭数据交换（同步方式）。
     *
     * <p>此方法会立即关闭数据交换，不等待服务端相应。不会阻塞当前线程。
     *
     * <p>该方法是幂等的：多次调用不会产生副作用。
     * 该方法满足 {@link Closeable} 接口契约，可用于 try-with-resources。
     *
     * @throws RuntimeException 如果关闭过程中发生错误（如 I/O 异常）
     */
    @Override
    void close();

    /**
     * 获取表示关闭完成状态的 {@link CompletionStage}。
     *
     * <p>无论通过 {@link #closing()} 还是 {@link #close()} 发起关闭，
     * 此 Stage 都会在底层连接真正释放后完成。
     *
     * <p>可用于注册关闭后的清理逻辑（如释放资源、更新状态等）。
     *
     * @return 关闭完成的异步通知对象；若已关闭，则返回已完成的 Stage
     */
    CompletionStage<Void> closeFuture();

    /**
     * 异步发送应用层数据。
     *
     * <p>数据将被序列化（由具体实现处理）并通过底层传输通道发送。
     * 调用本方法不保证数据已到达对端，仅表示已提交到发送队列。
     *
     * @param data 要发送的应用数据，不可为 {@code null}
     * @return 表示发送操作完成的 {@link CompletionStage}；
     * 成功完成时表示数据已写入底层缓冲区（不一定已送达），
     * 失败时包含异常（如连接中断、序列化失败等）
     * @throws IllegalStateException 如果数据交换已关闭
     * @throws NullPointerException  如果 {@code data} 为 {@code null}
     */
    CompletionStage<Void> send(T data);

    /**
     * 异步发送原始二进制数据。
     *
     * <p>直接发送 {@link ByteBuffer} 中的剩余字节，不进行额外编码。
     * 调用者应确保缓冲区在发送完成前内容不变（建议使用只读或复制缓冲区）。
     *
     * @param buffer 二进制数据缓冲区，不可为 {@code null}
     * @return 表示发送操作完成的 {@link CompletionStage}；
     * 成功完成时表示数据已写入底层缓冲区，
     * 失败时包含异常（如连接中断、写失败等）
     * @throws IllegalStateException 如果数据交换已关闭
     * @throws NullPointerException  如果 {@code buffer} 为 {@code null}
     */
    CompletionStage<Void> send(ByteBuffer buffer);


    /**
     * {@link Exchange} 的代理基类，用于装饰（Decorator）模式。
     *
     * <p>子类可通过继承此类，在不修改原始 {@link Exchange} 行为的前提下，
     * 添加日志、监控、权限校验、重试等横切逻辑。
     *
     * @param <T> 应用数据类型
     */
    class Proxy<T> implements Exchange<T> {

        private final Exchange<T> delegate;

        /**
         * 构造一个代理实例，包装给定的原始 {@link Exchange}。
         *
         * @param delegate 被代理的原始数据交换对象，不可为 {@code null}
         * @throws NullPointerException 如果 {@code origin} 为 {@code null}
         */
        protected Proxy(Exchange<T> delegate) {
            this.delegate = Objects.requireNonNull(delegate, "origin must not be null");
        }

        /**
         * 获取被代理的原始 {@link Exchange} 实例。
         *
         * @return 原始数据交换对象
         */
        protected Exchange<T> delegate() {
            return delegate;
        }

        @Override
        public String id() {
            return delegate.id();
        }

        @Override
        public boolean isClosed() {
            return delegate.isClosed();
        }

        @Override
        public CompletionStage<Void> closing() {
            return delegate.closing();
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public CompletionStage<Void> closeFuture() {
            return delegate.closeFuture();
        }

        @Override
        public CompletionStage<Void> send(T data) {
            return delegate.send(data);
        }

        @Override
        public CompletionStage<Void> send(ByteBuffer buffer) {
            return delegate.send(buffer);
        }

    }

    /**
     * 应用数据编解码器
     *
     * @param <T> 发送数据类型
     * @param <R> 接收数据类型
     */
    interface Codec<T, R> {

        /**
         * {@code T -> JSON}
         * 将发送数据编码为{@code JSON}
         *
         * @param data 发送数据
         * @return JSON
         */
        String encode(T data);

        /**
         * {@code JSON -> R}
         * 将收到的{@code JSON}解码为接收数据
         *
         * @param json 接收 JSON
         * @return R
         */
        R decode(String json);

    }

    /**
     * 数据交换连接的事件处理器，用于响应连接生命周期中的关键事件。
     *
     * <p>该接口定义了四个核心回调方法，分别对应：
     * 连接建立（{@code onOpen}）、应用数据接收（{@code onData}）、
     * 二进制数据接收（{@code onBinary}）以及连接关闭（{@code onClosed}）。
     *
     * <p>实现类应保证线程安全，因为这些方法可能被 I/O 线程或异步任务线程调用。
     * 所有返回 {@link CompletionStage} 的方法必须完成（无论是成功还是异常），
     * 否则可能导致资源泄漏或状态不一致。
     *
     * @param <T> 发送数据的类型（与 {@link Exchange} 的泛型一致）
     * @param <R> 接收数据的类型（应用层消息反序列化后的对象类型）
     */
    interface Handler<T, R> {

        /**
         * 当数据交换连接成功建立时被调用。
         *
         * <p>此方法在连接初始化完成后、任何数据接收之前调用一次。
         * 可在此处执行认证、发送初始握手消息、注册会话等操作。
         *
         * <p>若此方法抛出异常，通常会导致连接立即关闭，并触发 {@link #onClosed(Throwable)}。
         *
         * @param exchange 用于与对端通信的数据交换器；
         *                 可通过它发送消息或主动关闭连接；
         *                 保证非 {@code null}
         */
        void onOpen(Exchange<T> exchange);

        /**
         * 当接收到应用层数据（已反序列化为 {@code R} 类型）时被调用。
         *
         * <p>该方法用于处理业务消息。实现应尽快返回一个 {@link CompletionStage}，
         * 表示对该消息的处理已完成（例如：已写入数据库、已转发给其他服务等）。
         *
         * <p>如果返回的 {@code CompletionStage} 异常完成（failed），
         * 某些实现可能会将该异常视为严重错误并关闭连接；
         * 因此建议在内部捕获可恢复的异常，避免传播到 Stage 外。
         *
         * @param data 接收到的应用数据，由底层自动反序列化得到；
         *             保证非 {@code null}
         * @return 表示处理完成的异步结果；成功完成时结果为 {@code null}，
         * 异常完成时表示处理失败（可能影响连接稳定性）
         */
        CompletionStage<Void> onData(R data);

        /**
         * 当接收到原始二进制数据时被调用。
         *
         * <p>适用于需要直接处理字节流的场景（如文件传输、自定义协议等）。
         * 调用者应避免修改 {@code buffer} 的内容（建议只读访问），
         * 并注意缓冲区的有效范围（使用 {@code buffer.remaining()}）。
         *
         * <p>同 {@link #onData(Object)}，返回的 {@code CompletionStage} 应妥善完成。
         *
         * @param buffer 接收到的二进制数据缓冲区；
         *               保证非 {@code null}，且处于可读状态（position ≤ limit）
         * @return 表示二进制数据处理完成的异步结果；
         * 成功完成表示处理结束，异常完成可能触发连接关闭
         */
        CompletionStage<Void> onBinary(ByteBuffer buffer);

        /**
         * 当数据交换连接关闭时被调用（无论正常关闭或异常中断）。
         *
         * <p>此方法总是被调用，且仅调用一次。可用于释放资源、更新在线状态、记录日志等。
         * 连接关闭可能由以下原因触发：
         * <ul>
         *   <li>主动关闭：客户端或服务端主动断开连接</li>
         *   <li>异常关闭：网络中断、协议错误或在 {@link #onOpen(Exchange)}、{@link #onData(Object)}、
         *       {@link #onBinary(ByteBuffer)} 中抛出未捕获异常</li>
         *   <li>超时关闭：连接或心跳超时</li>
         * </ul>
         *
         * <p>参数说明：
         * <ul>
         *   <li>若因异常导致关闭（如网络中断、协议错误），{@code ex} 为非 {@code null}；</li>
         *   <li>若为主动或正常关闭（如客户端发送 close 帧），{@code ex} 通常为 {@code null}。</li>
         * </ul>
         *
         * <p><strong>重要提示：</strong>该方法不应抛出异常，否则可能被底层框架忽略或导致未定义行为。
         *
         * @param ex 导致连接关闭的异常；若为正常关闭，则为 {@code null}
         */
        void onClosed(Throwable ex);

    }


    /**
     * 代理数据交换处理器，用于将数据交换处理器转换为代理处理器。
     *
     * @param <T> 发送数据的类型（与 {@link Exchange} 的泛型一致）
     * @param <R> 接收到的数据类型（应用层消息反序列化后的对象类型）
     */
    class ProxyHandler<T, R> implements Handler<T, R> {

        private final Handler<T, R> delegate;

        /**
         * 创建代理数据交换处理器。
         *
         * @param delegate 数据交换处理器的实现
         */
        public ProxyHandler(Handler<T, R> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onOpen(Exchange<T> exchange) {
            delegate.onOpen(exchange);
        }

        @Override
        public CompletionStage<Void> onData(R data) {
            return delegate.onData(data);
        }

        @Override
        public CompletionStage<Void> onBinary(ByteBuffer buffer) {
            return delegate.onBinary(buffer);
        }

        @Override
        public void onClosed(Throwable ex) {
            delegate.onClosed(ex);
        }

    }

}