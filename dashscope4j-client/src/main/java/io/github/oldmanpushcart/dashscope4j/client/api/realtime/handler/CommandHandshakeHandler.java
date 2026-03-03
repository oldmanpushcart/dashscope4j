package io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.api.Ret;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.ToStringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 命令模式下的握手处理器
 * <p>
 * 部分实时交互的内部协议是通过命令模式驱动服务端任务完成，
 * 在这种模式下需要发送和处理任务启动、停止等命令，以此来完成数据流的处理。这个过程被抽象为握手。
 * </p>
 * <p>
 * 与{@code Dashscope}官网推荐不一样的是，基于命令模式的实时交互时可以通过开启新任务实现网络通道的复用。
 * 但这种复用会带来理解和连接管理成本的上升，而且绝大部分场景中都不需要对这个连接进行复用。所以这里就简化为：
 * {@code 连接:任务 = 1:1}的单连接单任务模式。
 * </p>
 * <p>偶尔处理就不需要复用，经常需要交互地应该用实时性更好地会话模式。</p>
 */
public class CommandHandshakeHandler implements Realtime.Handler<String, String> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Mode mode;
    private final Realtime.Session<?, ?> session;
    private final Realtime.Handler<String, String> handler;

    private final AtomicReference<State> state = new AtomicReference<>(State.AWAITING_HANDSHAKE);
    private final Map<Event.Type, CompletableFuture<Void>> futureMap = new ConcurrentHashMap<>();
    private volatile SessionEmitter emitter;
    private volatile Realtime.Emitter<String> origin;

    public CommandHandshakeHandler(Mode mode, Realtime.Session<?, ?> session, Realtime.Handler<String, String> handler) {
        this.mode = mode;
        this.session = session;
        this.handler = handler;
    }

    @Override
    public String toString() {
        return "dashscope4j-client://realtime";
    }

    @Override
    public void onOpen(Realtime.Emitter<String> origin) {

        this.origin = origin;
        this.emitter = new SessionEmitter(mode, origin, futureMap);

        /*
         * STEP1 - 发起握手
         */
        final var sessionPayload = JacksonJsonUtils.toJson(session);
        final var command = Command.of(origin.id(), mode, Command.Action.RUN, sessionPayload);
        origin.data(command.toJson());

    }

    @Override
    public void onData(String output) {
        final var event = JacksonJsonUtils.toObject(output, Event.class);
        final var header = event.header();

        if (!header.isSuccess()) {
            throw new IllegalStateException("Command handshake failed! code=%s;desc=%s".formatted(
                    header.code(),
                    header.desc()
            ));
        }

        final var type = event.header().type();
        final var future = futureMap.remove(type);
        if (null != future) {
            future.complete(null);
        }

        final var s = state.get();
        switch (s) {

            /*
             * STEP2 - 等待握手结果
             */
            case AWAITING_HANDSHAKE: {
                if (header.type() != Event.Type.STARTED) {
                    throw new IllegalStateException("Command handshake failed! Expect %s event, but was: %s".formatted(
                            Event.Type.STARTED,
                            header.type()
                    ));
                }
                if (!state.compareAndSet(s, State.HANDSHAKE_COMPLETED)) {
                    throw new IllegalStateException("Command handshake failed! Expect %s state, but was: %s".formatted(
                            s,
                            state.get()
                    ));
                }

                logger.debug("{}/{} command handshake completed.", this, emitter.id());
                handler.onOpen(emitter);
            }

            /*
             * STEP3 - 握手结束，开始正式通信
             */
            case HANDSHAKE_COMPLETED: {

                // 会话通讯
                if (header.type() == Event.Type.GENERATED) {
                    handler.onData(event.payload());
                }

                /*
                 * 对于 OUT 模式有一个特殊处理
                 *
                 * 该模式的结束行为并非由 emitter 触发，而是系统默认触发。
                 * OUT 模式下，处理完请求后，系统会主动发送 FINISHED 事件，请求结束本次TASK。
                 *
                 * 由于当前的抽象是 一个 Connection 只有一个 Task，所以当 Task FINISHED 的时候，Connection就应该被断开。
                 */
                if (mode == Mode.OUT && header.type() == Event.Type.FINISHED) {
                    logger.debug("{}/{} session finished.", this, emitter.id());
                    handler.onData(event.payload());
                    origin.close();
                }

            }
        }
        ;

    }

    @Override
    public void onBinary(ByteBuffer buffer) {
        handler.onBinary(buffer);
    }

    @Override
    public void onClosed(Throwable ex) {
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
        handler.onClosed(ex);
    }

    private static class SessionEmitter extends Realtime.DelegateEmitter<String> {

        private final Mode mode;
        private final Map<Event.Type, CompletableFuture<Void>> futureMap;

        private final AtomicBoolean finished = new AtomicBoolean(false);

        public SessionEmitter(Mode mode, Realtime.Emitter<String> delegate, Map<Event.Type, CompletableFuture<Void>> futureMap) {
            super(delegate);
            this.mode = mode;
            this.futureMap = futureMap;
        }

        @Override
        public Realtime.Emitter<String> data(String input) {
            final var command = Command.of(id(), mode, Command.Action.CONTINUE, input);
            super.data(command.toJson());
            return this;
        }

        @Override
        public void close() {

            if (!finished.compareAndSet(false, true)) {
                return;
            }

            /*
             * OUT 模式的特殊处理
             *
             * 对于该模式而言，是不需要主动发起 FINISH 请求进行优雅关闭的，
             * 而应该由服务端在 TASK 完成时主动发送 FINISHED 来结束这个连接。
             */
            if (mode == Mode.OUT) {
                return;
            }

            CompletableFuture.completedStage(null)
                    .thenCompose(unused -> {

                        final var finishF = new CompletableFuture<Void>();
                        futureMap.put(Event.Type.FINISHED, finishF);

                        final var command = Command.of(id(), mode, Command.Action.FINISH, "{\"input\": {}}");
                        super.data(command.toJson());

                        return finishF;
                    })
                    .whenComplete((unused, ex) -> {
                        futureMap.remove(Event.Type.FINISHED);
                        finished.compareAndSet(true, null != ex);
                        super.close();
                    });
        }

    }

    private enum State {
        AWAITING_HANDSHAKE,
        HANDSHAKE_COMPLETED
    }

    // --- COMMAND 协议
    record Command(

            @JsonProperty("header")
            Header header,

            @JsonRawValue
            @JsonProperty("payload")
            String payload

    ) {

        record Header(

                @JsonProperty("task_id")
                String id,

                @JsonProperty("streaming")
                Mode mode,

                @JsonProperty("action")
                Action action

        ) {

        }

        public enum Action {

            @JsonProperty("run-task")
            RUN,

            @JsonProperty("continue-task")
            CONTINUE,

            @JsonProperty("finish-task")
            FINISH

        }

        public String toJson() {
            return JacksonJsonUtils.toJson(this);
        }

        public static Command of(String id, Mode mode, Action action, String payload) {
            return new Command(new Header(id, mode, action), payload);
        }

    }

    record Event(

            @JsonProperty("header")
            Header header,

            @JsonDeserialize(using = ToStringDeserializer.class)
            @JsonProperty("payload")
            String payload

    ) {

        public static final class Header extends Ret {

            private final String id;
            private final Type type;

            @JsonCreator
            public Header(

                    @JsonProperty("task_id")
                    String id,

                    @JsonProperty("event")
                    Type type,

                    @JsonProperty("error_code")
                    String code,

                    @JsonProperty("error_message")
                    String desc

            ) {
                super(code, desc);
                this.id = id;
                this.type = type;
            }

            public String id() {
                return id;
            }

            public Type type() {
                return type;
            }

        }

        public enum Type {

            @JsonProperty("task-started")
            STARTED,

            @JsonProperty("result-generated")
            GENERATED,

            @JsonProperty("task-finished")
            FINISHED,

            @JsonProperty("task-failed")
            FAILED

        }

    }

    public enum Mode {

//        @JsonProperty("none")
//        ONCE,

        @JsonProperty("in")
        IN,

        @JsonProperty("out")
        OUT,

        @JsonProperty("duplex")
        DUPLEX

    }

}
