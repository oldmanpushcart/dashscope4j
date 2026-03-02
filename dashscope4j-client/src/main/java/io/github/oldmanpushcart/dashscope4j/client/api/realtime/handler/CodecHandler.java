package io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler;

import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime.Emitter;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * 输入输出编解码处理器
 *
 * @param <I>  输入类型
 * @param <O>  输出类型
 * @param <UI> 编码后输入类型
 * @param <UO> 解码后输出类型
 */
public class CodecHandler<I, O, UI, UO> implements Realtime.Handler<I, O> {

    private final Function<UI, I> encoder;
    private final Function<O, UO> decoder;
    private final Realtime.Handler<UI, UO> next;

    /**
     * 构造编解码处理器
     *
     * @param encoder 输入编码
     * @param decoder 输出解码
     * @param next    下游处理器
     */
    public CodecHandler(Function<UI, I> encoder, Function<O, UO> decoder, Realtime.Handler<UI, UO> next) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.next = next;
    }

    @Override
    public void onOpen(Emitter<I> emitter) {
        next.onOpen(new MapEmitter<>(encoder, emitter));
    }

    @Override
    public void onData(O output) {
        next.onData(decoder.apply(output));
    }

    @Override
    public void onBinary(ByteBuffer buffer) {
        next.onBinary(buffer);
    }

    @Override
    public void onClosed(Throwable ex) {
        next.onClosed(ex);
    }

    private record MapEmitter<I, UI>(Function<UI, I> mapper, Emitter<I> delegate)
            implements Emitter<UI> {


        @Override
        public Emitter<UI> data(UI input) {
            delegate.data(mapper.apply(input));
            return this;
        }

        @Override
        public Emitter<UI> binary(ByteBuffer buffer) {
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
     * 构建{@code Object <-> JSON}的编解码处理器
     *
     * @param inType  输入类型
     * @param outType 输出类型
     * @param handler 下游处理器
     * @param <I>     输入类型
     * @param <O>     输出类型
     * @return {@code JSON}编解码处理器
     */
    public static <I, O> CodecHandler<String, String, I, O> json(Class<I> inType, Class<O> outType, Realtime.Handler<I, O> handler) {
        return new CodecHandler<>(
                JacksonJsonUtils::toJson,
                s -> JacksonJsonUtils.toObject(s, outType),
                handler
        );
    }

}
