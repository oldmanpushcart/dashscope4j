package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.internal.handler;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeEmitter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.QwenAsrRealtimeSession;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.ClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.SessionFinishClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.client.SessionUpdateClientEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ErrorServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.ServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.SessionCreatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.asr.qwen_asr_realtime.event.server.SessionUpdatedServerEvent;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

public class SessionHandshakeHandler implements Realtime.Handler<ClientEvent, ServerEvent> {

    private static final String KEY_SESSION_FINISHED = "session.finished";

    private final QwenAsrRealtimeSession session;
    private final Realtime.Handler<ClientEvent, ServerEvent> delegate;

    private volatile Realtime.Emitter<ClientEvent> emitter;
    private final Map<String, CompletableFuture<?>> futureMap = new ConcurrentHashMap<>();
    private final AtomicReference<State> state = new AtomicReference<>(State.AWAITING_SESSION_CREATED);

    public SessionHandshakeHandler(QwenAsrRealtimeSession session, Realtime.Handler<ClientEvent, ServerEvent> delegate) {
        this.session = session;
        this.delegate = delegate;
    }

    @Override
    public void onOpen(Realtime.Emitter<ClientEvent> emitter) {
        this.emitter = emitter;
    }

    @Override
    public void onData(ServerEvent output) {

        /*
         * 根据事件类型从回调池中寻找对应的回调，并通知其完成。
         */
        final var type = output.type();
        final var future = futureMap.remove(type);
        if (null != future) {
            future.complete(null);
        }

        /*
         * 这里对错误事件进行优先处理。
         * 抛出异常，触发连接关闭。
         */
        if (output instanceof ErrorServerEvent event) {
            final var error = event.error();
            throw new IllegalStateException("Server error! code=%s;desc=%s".formatted(
                    error.code(),
                    error.message()
            ));
        }

        final var s = state.get();
        switch (s) {

            /*
             * 连接建立后，第一个永远是会话创建事件
             */
            case AWAITING_SESSION_CREATED -> {
                if (!(output instanceof SessionCreatedServerEvent)) {
                    throw new IllegalStateException("Expect session.created event, but was: " + output.type());
                }
                if (!state.compareAndSet(s, State.AWAITING_SESSION_CONFIRMED)) {
                    throw new IllegalStateException("Expect %s state, but was: %s".formatted(s, state.get()));
                }
                emitter.data(new SessionUpdateClientEvent(genUUID22(), session));
            }

            /*
             * 第二个收到的事件必定是会话更新事件
             */
            case AWAITING_SESSION_CONFIRMED -> {
                if (!(output instanceof SessionUpdatedServerEvent event)) {
                    throw new IllegalStateException("Expect session.updated event, but was: " + output.type());
                }
                if (!state.compareAndSet(s, State.HANDSHAKE_COMPLETED)) {
                    throw new IllegalStateException("Expect %s state, but was: %s".formatted(s, state.get()));
                }

                final var session = event.session();
                final var newSession = QwenAsrRealtimeSession.newBuilder(session)
                        .model(session.model())
                        .build();
                final var qwenAsrRealtimeEmitter = new QwenAsrRealtimeEmitterImpl(emitter, newSession, futureMap);
                delegate.onOpen(qwenAsrRealtimeEmitter);
            }

            /*
             * 后续事件无论是什么都转发下游处理
             */
            case HANDSHAKE_COMPLETED -> delegate.onData(output);
        }
    }

    @Override
    public void onBinary(ByteBuffer buffer) {
        delegate.onBinary(buffer);
    }

    @Override
    public void onClosed(Throwable ex) {

        // 连接关闭，通知并清理所有回调
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

        // 转发下游
        delegate.onClosed(ex);

    }

    /**
     * 握手状态
     * <p>
     * 握手过程：连接建立后，会立即收到会话创建消息，此时客户端必须根据自身配置强制更新一次会话，确保会话配置生效。
     * </p>
     * <p>
     * 状态图：{@code AWAITING_SESSION_CREATED -> AWAITING_SESSION_CONFIRMED -> HANDSHAKE_COMPLETED}
     * </p>
     */
    private enum State {

        /**
         * 等待 session.created
         */
        AWAITING_SESSION_CREATED,

        /**
         * 确认 session.updated
         */
        AWAITING_SESSION_CONFIRMED,

        /**
         * 握手完成
         */
        HANDSHAKE_COMPLETED,

    }

    private static class QwenAsrRealtimeEmitterImpl
            extends Realtime.DelegateEmitter<ClientEvent>
            implements QwenAsrRealtimeEmitter {

        private final QwenAsrRealtimeSession session;
        private final Map<String, CompletableFuture<?>> futureMap;

        public QwenAsrRealtimeEmitterImpl(Realtime.Emitter<ClientEvent> delegate, QwenAsrRealtimeSession session, Map<String, CompletableFuture<?>> futureMap) {
            super(delegate);
            this.session = session;
            this.futureMap = futureMap;
        }

        @Override
        public QwenAsrRealtimeSession session() {
            return session;
        }

        /**
         * 优雅关闭
         * <p>
         * 优雅关闭的流程如下
         *     <ul>
         *         <li>STEP1 - 客户端：发起结束申请</li>
         *         <li>STEP2 - 服务端：立即提交缓存并完成后续所有输入</li>
         *         <li>STEP3 - 服务端：响应结束</li>
         *         <li>STEP4 - 客户端：发起连接关闭</li>
         *         <li>STEP5 - 服务端：响应连接关闭</li>
         *     </ul>
         * </p>
         */
        @Override
        public void close() {

            // 如果已关闭，则幂等
            if (isClosed()) {
                return;
            }

            /*
             * 同一时间只能有一个结束操作。
             * 如果已经存在则主动避让，由进行中的结束操作完成后续关闭动作。
             */
            final var finishF = new CompletableFuture<>();
            if (futureMap.putIfAbsent(KEY_SESSION_FINISHED, finishF) != null) {
                return;
            }

            CompletableFuture.completedStage(null)

                    // 发送并等待结束动作完成
                    .thenCompose(unused -> {
                        data(new SessionFinishClientEvent(genUUID22()));
                        return finishF;
                    })

                    // 无论结束的结果如何，都必须走到关闭流程
                    .handle((unused, ex) -> {
                        futureMap.remove(KEY_SESSION_FINISHED);
                        super.close();
                        return null;
                    });

        }
    }

}
