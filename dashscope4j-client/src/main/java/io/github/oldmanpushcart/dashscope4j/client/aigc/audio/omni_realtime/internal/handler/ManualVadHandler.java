package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.internal.handler;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeEmitter.ManualVad;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.OmniRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.client.*;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.omni_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

public class ManualVadHandler implements Realtime.Handler<ClientEvent, ServerEvent> {

    private static final String KEY_BUFFER_CLEARED = "input_audio_buffer.cleared";
    private static final String KEY_BUFFER_COMMITTED = "input_audio_buffer.committed";
    private static final String KEY_RESPONSE_CREATED = "response.created";
    private static final String KEY_RESPONSE_DONE = "response.done";

    private final Map<String, CompletableFuture<?>> futureMap = new ConcurrentHashMap<>();
    private final Realtime.Handler<ClientEvent, ServerEvent> delegate;

    public ManualVadHandler(Realtime.Handler<ClientEvent, ServerEvent> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onOpen(Realtime.Emitter<ClientEvent> emitter) {
        final var manualVad = new ManualVadImpl((OmniRealtimeEmitter) emitter, futureMap);
        delegate.onOpen(manualVad);
    }

    @Override
    public void onData(ServerEvent output) {
        final var type = output.type();
        final var future = futureMap.remove(type);
        if (null != future) {
            future.complete(null);
        }
        delegate.onData(output);
    }

    @Override
    public void onBinary(ByteBuffer buffer) {

    }

    @Override
    public void onClosed(Throwable ex) {
        futureMap.forEach((type, future) -> {
            if (null != future) {
                future.completeExceptionally(ex);
            }
        });
        futureMap.clear();
        delegate.onClosed(ex);
    }


    private static class ManualVadImpl
            extends Realtime.DelegateEmitter<ClientEvent>
            implements ManualVad {

        private final OmniRealtimeEmitter origin;
        private final Map<String, CompletableFuture<?>> futureMap;
        private final AtomicReference<State> state = new AtomicReference<>(State.IDLE);

        private ManualVadImpl(OmniRealtimeEmitter origin, Map<String, CompletableFuture<?>> futureMap) {
            super(origin);
            this.origin = origin;
            this.futureMap = futureMap;
        }

        @Override
        public OmniRealtimeSession session() {
            return origin.session();
        }

        @Override
        public InputOp newInput() {

            // 从空闲切换到可输入，同一时间只能有一个缓存可被输入
            if (!state.compareAndSet(State.IDLE, State.INPUT)) {
                throw new IllegalStateException("Expect state %s, but was %s".formatted(
                        State.IDLE,
                        state.get()
                ));
            }

            // 返回输入操作
            return new InputOpImpl();

        }

        /**
         * 输入操作
         */
        private class InputOpImpl implements InputOp {

            /*
             * 已输入标记
             * 标记整个流程中是否存在用户的输入行为
             */
            private volatile boolean inputted = false;

            /*
             * 中止标记
             * 输入操作如被中止，则无法继续操作
             */
            private final AtomicBoolean terminated = new AtomicBoolean(false);

            @Override
            public InputOp image(ByteBuffer image) {

                if (terminated.get()) {
                    throw new IllegalStateException("Already terminated!");
                }

                if (state.get() != State.INPUT) {
                    throw new IllegalStateException("Expect state %s, but was %s".formatted(
                            State.INPUT,
                            state.get()
                    ));
                }

                origin.data(new BufferAppendImageClientEvent(genUUID22(), image));
                inputted = true;
                return this;
            }

            @Override
            public InputOp audio(ByteBuffer buffer) {

                if (terminated.get()) {
                    throw new IllegalStateException("Already terminated!");
                }

                if (state.get() != State.INPUT) {
                    throw new IllegalStateException("Expect state %s, but was %s".formatted(
                            State.INPUT,
                            state.get()
                    ));
                }

                origin.data(new BufferAppendAudioClientEvent(genUUID22(), buffer));
                inputted = true;
                return this;
            }

            @Override
            public CompletionStage<InputOp> clearAsync() {
                return clearTo(State.INPUT)
                        .thenApply(v -> this);
            }

            @Override
            public CompletionStage<Void> cancelAsync() {
                return clearTo(State.IDLE);
            }

            /**
             * 清空输入缓存并回到指定状态
             *
             * @param target 指定状态
             * @return 清理回调
             */
            private CompletionStage<Void> clearTo(State target) {

                if (terminated.get()) {
                    throw new IllegalStateException("Already terminated!");
                }

                if (!state.compareAndSet(State.INPUT, State.CLEAR)) {
                    throw new IllegalStateException("Expect state %s, but was %s".formatted(
                            State.INPUT,
                            state.get()
                    ));
                }

                // 在没有任何输入之前，clear 操作无效
                if (!inputted) {
                    state.compareAndSet(State.CLEAR, target);
                    return CompletableFuture.completedStage(null);
                }

                return CompletableFuture.completedStage(null)
                        .thenCompose(unused -> {
                            final var clearF = new CompletableFuture<Void>();
                            futureMap.put(KEY_BUFFER_CLEARED, clearF);
                            origin.data(new BufferClearClientEvent(genUUID22()));
                            return clearF;
                        })
                        .whenComplete((v, ex) -> {
                            futureMap.remove(KEY_BUFFER_CLEARED);
                            state.compareAndSet(State.CLEAR, null != ex ? State.INPUT : target);
                        })
                        .thenAccept(unused -> inputted = false);

            }

            @Override
            public CompletionStage<ResponseOp> commitAsync() {

                if (terminated.get()) {
                    throw new IllegalStateException("Already terminated!");
                }

                if (!state.compareAndSet(State.INPUT, State.COMMIT)) {
                    throw new IllegalStateException("Expect state %s, but was %s".formatted(
                            State.INPUT,
                            state.get()
                    ));
                }

                return CompletableFuture.completedStage(null)
                        .thenCompose(unused -> {
                            final var commitF = new CompletableFuture<Void>();
                            futureMap.put(KEY_BUFFER_COMMITTED, commitF);
                            origin.data(new BufferCommitClientEvent(genUUID22()));
                            return commitF;
                        })
                        .whenComplete((v, ex) -> {
                            futureMap.remove(KEY_BUFFER_COMMITTED);
                            state.compareAndSet(State.COMMIT, null != ex ? State.INPUT : State.COMMITTED);
                        })
                        .thenApply(unused -> new ResponseOpImpl(terminated));
            }

        }

        private class ResponseOpImpl implements ResponseOp {

            private final AtomicBoolean terminated;

            private ResponseOpImpl(AtomicBoolean terminated) {
                this.terminated = terminated;
            }

            @Override
            public CompletionStage<Void> createAsync() {

                if (terminated.get()) {
                    throw new IllegalStateException("Already terminated!");
                }

                if (!state.compareAndSet(State.COMMITTED, State.RESPONSE)) {
                    throw new IllegalStateException("Expect state %s, but was %s".formatted(
                            State.COMMITTED,
                            state.get()
                    ));
                }

                return CompletableFuture.completedStage(null)

                        /*
                         * 创建并订阅 CREATE 和 DONE 回调
                         *
                         * 因为 CREATE 和 DONE 是顺序发生，而且中间无任何状态转换，所以必须在发送 create 请求之前完成这两个事件的注册。
                         * 同时要求 CREATE 必须先于 DONE 发生。
                         */
                        .thenCompose(unused -> {

                            final var createF = new CompletableFuture<Void>();
                            futureMap.put(KEY_RESPONSE_CREATED, createF);

                            final var doneF = new CompletableFuture<Void>();
                            futureMap.put(KEY_RESPONSE_DONE, doneF);

                            origin.data(new ResponseCreateClientEvent(genUUID22()));
                            return createF.thenCompose(u -> doneF);

                        })

                        /*
                         * 无论是 CREATE 还是 DONE 回调导致的错误，都意味着整个响应生成失败。
                         * 需要回滚状态到响应状态。
                         */
                        .whenComplete((unused, ex) -> {
                            futureMap.remove(KEY_RESPONSE_CREATED);
                            futureMap.remove(KEY_RESPONSE_DONE);
                            state.compareAndSet(State.RESPONSE, null != ex ? State.COMMITTED : State.IDLE);
                        })

                        // 标记整个InputOp->ResponseOp流程已经结束
                        .thenAccept(unused -> terminated.set(true));
            }

            @Override
            public void cancel() {

                if (terminated.get()) {
                    throw new IllegalStateException("Already terminated!");
                }

                // 如果没有正在进行中的响应生成，则不用取消。
                if (state.get() != State.RESPONSE) {
                    return;
                }

                /*
                 * 直接发送取消请求，后续不用管理。
                 * 由 create 流程处理好状态流转。
                 */
                origin.data(new ResponseCancelClientEvent(genUUID22()));

            }

        }


        /**
         * 会话状态机
         */
        private enum State {

            /**
             * 会话空闲
             */
            IDLE,

            /**
             * 缓存可输入
             */
            INPUT,

            /**
             * 缓存清除中
             */
            CLEAR,

            /**
             * 缓存提交中
             */
            COMMIT,

            /**
             * 缓存已提交
             */
            COMMITTED,

            /**
             * 响应生成中
             */
            RESPONSE

        }

    }

}
