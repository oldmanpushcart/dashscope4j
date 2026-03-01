package io.github.oldmanpushcart.dashscope4j.client.api.realtime;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Handler 链式转换器
 * <p>
 * 用于在不同类型的 Handler 之间进行转换和组合，支持输入输出类型的独立映射。
 * </p>
 *
 * <h2>类型参数</h2>
 * <ul>
 *   <li>{@code I} - 最终 Handler 的输入类型</li>
 *   <li>{@code O} - 最终 Handler 的输出类型</li>
 *   <li>{@code UI} - 输入 Handler 的输入类型</li>
 *   <li>{@code UO} - 输入 Handler 的输出类型</li>
 * </ul>
 *
 * <h2>使用示例</h2>
 * <pre>{@code
 * var chain = HandlerChain.<String, String>identity()
 *     .then(h -> new CommandHandler(h))
 *     .mapInput(JacksonJsonUtils::toJson)
 *     .mapOutput(s -> JacksonJsonUtils.toObject(s, Response.class))
 *     .filterOutput(r -> r.isValid())
 *     .build(myHandler);
 * }</pre>
 *
 * @param <I>  最终输入类型
 * @param <O>  最终输出类型
 * @param <UI> 用户输入类型
 * @param <UO> 用户输出类型
 */
public class HandlerChain<I, O, UI, UO> {

    private final Function<Realtime.Handler<UI, UO>, Realtime.Handler<I, O>> transformer;

    /**
     * 创建 HandlerChain
     *
     * @param transformer 转换函数
     */
    public HandlerChain(Function<Realtime.Handler<UI, UO>, Realtime.Handler<I, O>> transformer) {
        this.transformer = Objects.requireNonNull(transformer, "transformer must not be null");
    }

    /**
     * 添加转换步骤
     *
     * @param transformer 转换函数
     * @param <NI>        新地输入类型
     * @param <NO>        新地输出类型
     * @return 新的 HandlerChain
     */
    public <NI, NO> HandlerChain<I, O, NI, NO> then(Function<Realtime.Handler<NI, NO>, Realtime.Handler<UI, UO>> transformer) {
        Objects.requireNonNull(transformer, "transformer must not be null");
        return new HandlerChain<>(ninoHandler -> {
            final var uiuoHandler = transformer.apply(ninoHandler);
            return this.transformer.apply(uiuoHandler);
        });
    }

    /**
     * 过滤输出数据
     *
     * @param filter 输出过滤器
     * @return 当前 HandlerChain
     */
    public HandlerChain<I, O, UI, UO> filterOutput(Predicate<UO> filter) {
        Objects.requireNonNull(filter, "filter must not be null");
        return then(uiuoHandler -> new FilterHandler<>(filter, uiuoHandler));
    }

    private record FilterHandler<UI, UO>(
            Predicate<UO> filter,
            Realtime.Handler<UI, UO> handler
    ) implements Realtime.Handler<UI, UO> {

        @Override
        public void onOpen(Realtime.Emitter<UI> emitter) {
            handler.onOpen(emitter);
        }

        @Override
        public void onData(UO output) {
            if (filter.test(output)) {
                handler.onData(output);
            }
        }

        @Override
        public void onBinary(ByteBuffer buffer) {
            handler.onBinary(buffer);
        }

        @Override
        public void onClosed(Throwable ex) {
            handler.onClosed(ex);
        }

    }

    /**
     * 映射输出数据
     *
     * @param mapper 输出映射函数
     * @param <NO>   新地输出类型
     * @return 新的 HandlerChain
     */
    public <NO> HandlerChain<I, O, UI, NO> mapOutput(Function<UO, NO> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        return then(uiuoHandler -> new MapOutputHandler<>(mapper, uiuoHandler));
    }

    private record MapOutputHandler<UI, UO, NO>(
            Function<UO, NO> mapper,
            Realtime.Handler<UI, NO> handler
    ) implements Realtime.Handler<UI, UO> {

        @Override
        public void onOpen(Realtime.Emitter<UI> emitter) {
            handler.onOpen(emitter);
        }

        @Override
        public void onData(UO output) {
            handler.onData(mapper.apply(output));
        }

        @Override
        public void onBinary(ByteBuffer buffer) {
            handler.onBinary(buffer);
        }

        @Override
        public void onClosed(Throwable ex) {
            handler.onClosed(ex);
        }

    }

    /**
     * 映射输入数据
     *
     * @param mapper 输入映射函数
     * @param <NI>   新地输入类型
     * @return 新的 HandlerChain
     */
    public <NI> HandlerChain<I, O, NI, UO> mapInput(Function<NI, UI> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        return then(ninoHandler -> new MapInputHandler<>(mapper, ninoHandler));
    }

    private record MapInputHandler<UI, UO, NI>(
            Function<NI, UI> mapper,
            Realtime.Handler<NI, UO> handler
    ) implements Realtime.Handler<UI, UO> {

        @Override
        public void onOpen(Realtime.Emitter<UI> emitter) {
            handler.onOpen(new Realtime.Emitter<>() {
                @Override
                public void data(NI input) {
                    emitter.data(mapper.apply(input));
                }

                @Override
                public void binary(ByteBuffer buffer) {
                    emitter.binary(buffer);
                }

                @Override
                public void closing() {
                    emitter.closing();
                }

                @Override
                public void closing(Throwable ex) {
                    emitter.closing(ex);
                }

                @Override
                public String id() {
                    return emitter.id();
                }

                @Override
                public boolean isClosed() {
                    return emitter.isClosed();
                }

                @Override
                public void close() {
                    emitter.close();
                }

                @Override
                public CompletionStage<Void> closeFuture() {
                    return emitter.closeFuture();
                }
            });
        }

        @Override
        public void onData(UO output) {
            handler.onData(output);
        }

        @Override
        public void onBinary(ByteBuffer buffer) {
            handler.onBinary(buffer);
        }

        @Override
        public void onClosed(Throwable ex) {
            handler.onClosed(ex);
        }

    }


    public <NI> HandlerChain<I, O, NI, UO> mapEmitter(Function<Realtime.Emitter<UI>, Realtime.Emitter<NI>> mapper) {
        Objects.requireNonNull(mapper, "mapper must not be null");
        return then(uiuoHandler -> new MapEmitterHandler<>(mapper, uiuoHandler));
    }

    private record MapEmitterHandler<UI, UO, NI>(
            Function<Realtime.Emitter<UI>, Realtime.Emitter<NI>> mapper,
            Realtime.Handler<NI, UO> handler
    ) implements Realtime.Handler<UI, UO> {

        @Override
        public void onOpen(Realtime.Emitter<UI> emitter) {
            final var newEmitter = mapper.apply(emitter);
            handler.onOpen(newEmitter);
        }

        @Override
        public void onData(UO output) {
            handler.onData(output);
        }

        @Override
        public void onBinary(ByteBuffer buffer) {
            handler.onBinary(buffer);
        }

        @Override
        public void onClosed(Throwable ex) {
            handler.onClosed(ex);
        }

    }

    /**
     * 同时映射输入和输出
     *
     * @param inputMapper  输入映射函数
     * @param outputMapper 输出映射函数
     * @param <NI>         新地输入类型
     * @param <NO>         新地输出类型
     * @return 新的 HandlerChain
     */
    public <NI, NO> HandlerChain<I, O, NI, NO> map(Function<NI, UI> inputMapper, Function<UO, NO> outputMapper) {
        return this
                .mapInput(inputMapper)
                .mapOutput(outputMapper);
    }

    /**
     * 构建最终的 Handler
     *
     * @param handler 用户提供的 Handler
     * @return 经过链式转换后的 Handler
     */
    public Realtime.Handler<I, O> build(Realtime.Handler<UI, UO> handler) {
        Objects.requireNonNull(handler, "handler must not be null");
        return transformer.apply(handler);
    }

    /**
     * 创建恒等链（不做任何转换）
     *
     * @param <I> 输入类型
     * @param <O> 输出类型
     * @return HandlerChain
     */
    public static <I, O> HandlerChain<I, O, I, O> identity() {
        return new HandlerChain<>(h -> h);
    }

}
