package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.internal.handler;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.BufferAppendAudioClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.BufferCommitClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

public class ManualVadHandler implements Realtime.Handler<ClientEvent, ServerEvent> {

    private static final String KEY_BUFFER_COMMITTED = "input_audio_buffer.committed";

    private final Realtime.Handler<ClientEvent, ServerEvent> delegate;
    private final Map<String, CompletableFuture<?>> futureMap = new ConcurrentHashMap<>();

    public ManualVadHandler(Realtime.Handler<ClientEvent, ServerEvent> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onOpen(Realtime.Emitter<ClientEvent> emitter) {
        final var manualVad = new ManualVadImpl(futureMap, (QwenAsrRealtimeEmitter) emitter);
        delegate.onOpen(manualVad);
    }

    @Override
    public void onData(ServerEvent event) {

        /*
         * 根据事件类型从回调池中寻找对应的回调，并通知其完成。
         */
        final var type = event.type();
        final var future = futureMap.remove(type);
        if (null != future) {
            future.complete(null);
        }

        // 向下转发事件
        delegate.onData(event);

    }

    @Override
    public void onBinary(ByteBuffer buffer) {
        delegate.onBinary(buffer);
    }

    @Override
    public void onClosed(Throwable ex) {

        /*
         * 连接要关闭了，覆巢之下无完卵。
         * 通知回调池中所有回调完成
         */
        futureMap.forEach((type, future) -> {
            if (null != future) {
                if (null != ex) {
                    future.completeExceptionally(ex);
                } else {
                    future.cancel(true);
                }
            }
        });
        futureMap.clear();

        // 向下转发关闭
        delegate.onClosed(ex);

    }


    private static class ManualVadImpl
            extends Realtime.DelegateEmitter<ClientEvent>
            implements QwenAsrRealtimeEmitter.ManualVad {

        private final AtomicReference<State> state = new AtomicReference<>(State.IDLE);
        private final Map<String, CompletableFuture<?>> futureMap;
        private final QwenAsrRealtimeEmitter delegate;

        public ManualVadImpl(Map<String, CompletableFuture<?>> futureMap, QwenAsrRealtimeEmitter delegate) {
            super(delegate);
            this.futureMap = futureMap;
            this.delegate = delegate;
        }

        @Override
        public QwenAsrRealtimeSession session() {
            return delegate.session();
        }

        @Override
        public InputOp newInput() {

            /*
             * 状态切换：IDLE -> INPUT
             *
             * 1. 只有空闲状态才能转到输入状态
             * 2. 同一时间只能有一个输入操作
             */
            if (!state.compareAndSet(State.IDLE, State.INPUT)) {
                throw new IllegalStateException("Expect state %s, but was %s".formatted(
                        State.IDLE,
                        state.get()
                ));
            }
            return new InputOpImpl();
        }

        private class InputOpImpl implements InputOp {

            /*
             * 中止标记
             * 输入操作如被中止，则无法继续操作
             */
            private volatile boolean terminated = false;

            @Override
            public InputOp audio(ByteBuffer buffer) {

                if (terminated) {
                    throw new IllegalStateException("Already terminated!");
                }

                /*
                 * 不能发送空数据包，不然云端会返回报错。
                 * 这里进行空数据包的兼容：直接认为发送成功。
                 */
                if (!buffer.hasRemaining()) {
                    return this;
                }

                //noinspection resource
                data(new BufferAppendAudioClientEvent(genUUID22(), buffer));
                return this;
            }

            @Override
            public CompletionStage<Void> commitAsync() {

                if (terminated) {
                    throw new IllegalStateException("Already terminated!");
                }

                if (!state.compareAndSet(State.INPUT, State.COMMIT)) {
                    throw new IllegalStateException("Expect state %s, but was %s".formatted(
                            State.INPUT,
                            state.get()
                    ));
                }

                return CompletableFuture.completedStage(null)

                        /*
                         * 发送提交请求
                         */
                        .thenCompose(unused -> {
                            final var commitF = new CompletableFuture<>();
                            futureMap.put(KEY_BUFFER_COMMITTED, commitF);
                            //noinspection resource
                            data(new BufferCommitClientEvent(genUUID22()));
                            return commitF;
                        })

                        /*
                         * 等待提交响应
                         * 如果出错则需要回滚状态到之前
                         */
                        .whenComplete((unused, ex) -> {
                            futureMap.remove(KEY_BUFFER_COMMITTED);
                            state.compareAndSet(State.COMMIT, null != ex ? State.INPUT : State.IDLE);
                        })

                        /*
                         * 完成并标记已提交。
                         * 提交完成后，该输入操作已完成，不能再使用。
                         */
                        .thenAccept(unused -> terminated = true);
            }

        }


        /**
         * 状态
         * <p>
         * 状态图：{@code IDLE -> INPUT -> COMMIT -> IDLE}
         * <ul>
         *     <li>1. 状态为连接独占设计</li>
         *     <li>2. 状态不可回头，COMMIT失败会回滚到INPUT状态</li>
         * </ul>
         * </p>
         */
        private enum State {
            IDLE,
            INPUT,
            COMMIT
        }

    }

}
