package io.github.oldmanpushcart.dashscope4j.client.api.realtime;

import io.github.oldmanpushcart.dashscope4j.client.api.Model;
import io.github.oldmanpushcart.dashscope4j.common.util.CompletableFutureUtils;

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
         * 关闭连接
         */
        @Override
        void close();

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
         * @return 发送回调
         */
        CompletionStage<Void> data(I in);

        /**
         * 发送数据集
         *
         * @param ins 数据集
         * @return 发送回调
         */
        default CompletionStage<Void> data(List<I> ins) {
            return CompletableFutureUtils
                    .sequentialMap(ins, this::data)
                    .thenAccept(unused -> {
                    });
        }

        /**
         * 发送二进制数据
         *
         * @param buffer 二进制数据
         * @return 发送回调
         */
        CompletionStage<Void> binary(ByteBuffer buffer);

        /**
         * 发送二进制数据集
         *
         * @param buffers 二进制数据集
         * @return 发送回调
         */
        default CompletionStage<Void> binary(Collection<ByteBuffer> buffers) {
            return CompletableFutureUtils
                    .sequentialMap(buffers, this::binary)
                    .thenAccept(unused -> {
                    });
        }

        /**
         * 发送关闭连接（正常关闭）
         *
         * @return 发送回调
         */
        CompletionStage<Void> closing();

        /**
         * 发送关闭连接（异常关闭）
         *
         * @param ex 异常
         * @return 发送回调
         */
        CompletionStage<Void> closing(Throwable ex);

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
        public CompletionStage<Void> data(I input) {
            return delegate.data(input);
        }

        @Override
        public CompletionStage<Void> binary(ByteBuffer buffer) {
            return delegate.binary(buffer);
        }

        @Override
        public CompletionStage<Void> closing() {
            return delegate.closing();
        }

        @Override
        public CompletionStage<Void> closing(Throwable ex) {
            return delegate.closing(ex);
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
        public void close() {
            delegate.close();
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
         * @return 处理回调
         */
        CompletionStage<Void> onData(O output);

        /**
         * 接收到二进制数据时触发
         *
         * @param buffer 二进制数据
         * @return 处理回调
         */
        CompletionStage<Void> onBinary(ByteBuffer buffer);

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
        Function<Handler<I, O>, Handler<I, O>> provider();

    }

}
