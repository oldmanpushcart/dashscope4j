package io.github.oldmanpushcart.dashscope4j.client.api.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.Model;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * 实时交互
 */
public interface Realtime {

    /**
     * 连接
     */
    interface Connection extends AutoCloseable {

        /**
         * @return 连接 ID
         */
        String id();

        /**
         * @return 是否已关闭
         */
        boolean isClosed();

        /**
         * 正常关闭连接
         */
        @Override
        void close();

        /**
         * 异常关闭连接
         *
         * @param ex 异常
         */
        void close(Throwable ex);

        /**
         * @return 连接关闭回调
         */
        CompletionStage<Void> closeFuture();

    }

    /**
     * 数据发送器
     *
     * @param <I> 输入类型
     */
    interface Emitter<I> extends Connection {

        /**
         * 发送数据
         *
         * @param in 数据
         */
        Emitter<I> data(I in);

        /**
         * 发送数据集
         *
         * @param ins 数据集
         */
        default Emitter<I> data(List<I> ins) {
            ins.forEach(this::data);
            return this;
        }

        /**
         * 发送二进制数据
         *
         * @param buffer 二进制数据
         */
        Emitter<I> binary(ByteBuffer buffer);

        /**
         * 发送二进制数据集
         *
         * @param buffers 二进制数据集
         */
        default Emitter<I> binary(Collection<ByteBuffer> buffers) {
            buffers.forEach(this::binary);
            return this;
        }

    }

    /**
     * 发送器代理
     *
     * @param <I> 输入类型
     */
    class DelegateEmitter<I> implements Emitter<I> {

        private final Emitter<I> delegate;

        public DelegateEmitter(Emitter<I> delegate) {
            this.delegate = delegate;
        }

        @Override
        public Emitter<I> data(I input) {
            delegate.data(input);
            return this;
        }

        @Override
        public Emitter<I> binary(ByteBuffer buffer) {
            delegate.binary(buffer);
            return this;
        }

        @Override
        public void close() {
            delegate.close();
        }

        @Override
        public void close(Throwable ex) {
            delegate.close(ex);
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
        public CompletionStage<Void> closeFuture() {
            return delegate.closeFuture();
        }

    }

    /**
     * 数据接收器
     *
     * @param <I> 输入类型
     * @param <O> 输出类型
     */
    interface Handler<I, O> {

        /**
         * 连接打开时触发
         *
         * @param emitter 数据发送器
         */
        void onOpen(Emitter<I> emitter);

        /**
         * 接收到数据时触发
         *
         * @param output 输出数据
         */
        void onData(O output);

        /**
         * 接收到二进制数据时触发
         *
         * @param buffer 二进制数据
         */
        void onBinary(ByteBuffer buffer);

        /**
         * 连接关闭时触发
         *
         * @param ex 异常
         *           <p>如果正常关闭，则异常为 null</p>
         */
        void onClosed(Throwable ex);

    }

    /**
     * 会话
     *
     * @param <I> 输入类型
     * @param <O> 输出类型
     */
    interface Session<I, O> {

        /**
         * @return 模型
         */
        Model<I, O> model();

        /**
         * @return 处理器提供者
         */
        Function<Handler<I, O>, Handler<String, String>> provider();

    }

}
