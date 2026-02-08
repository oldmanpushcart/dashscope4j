package io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.api.Ret;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.ToStringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

public class CommandHandshakeHandler implements Realtime.Handler<String, String> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Mode mode;
    private final Realtime.Session<?, ?> session;
    private final Realtime.Handler<String, String> handler;

    private final AtomicReference<State> state = new AtomicReference<>(State.AWAITING_HANDSHAKE);
    private volatile SessionEmitter emitter;

    public CommandHandshakeHandler(Mode mode, Realtime.Session<?, ?> session, Realtime.Handler<String, String> handler) {
        this.mode = mode;
        this.session = session;
        this.handler = handler;
    }

    @Override
    public String toString() {
        return "dashscope4j-client://realtime/command";
    }

    @Override
    public void onOpen(Realtime.Emitter<String> emitter) {
        this.emitter = new SessionEmitter(mode, emitter);

        /*
         * STEP1 - 发起握手
         */
        this.emitter.start(session)
                .whenComplete((u, ex) -> {
                    if (null != ex) {
                        logger.warn("{}/{} start failed!", this, emitter.id(), ex);
                        emitter.close();
                    } else {
                        logger.debug("{}/{} started.", this, emitter.id());
                    }
                });
    }

    @Override
    public CompletionStage<Void> onData(String output) {
        final var event = JacksonJsonUtils.toObject(output, Event.class);
        final var header = event.header();

        if (!header.isSuccess()) {
            logger.warn("{}/{} handshake failed! code={};desc={};",
                    this,
                    emitter.id(),
                    header.code(),
                    header.desc()
            );
            return CompletableFuture.failedStage(new IllegalStateException("Command handshake failed! code=%s;desc=%s".formatted(
                    header.code(),
                    header.desc()
            )));
        }

        final var s = state.get();
        return switch (s) {

            /*
             * STEP2 - 等待握手结果
             */
            case AWAITING_HANDSHAKE: {
                if (header.type() != Event.Type.STARTED) {
                    throw new IllegalStateException("Handshake failed! expect %s event, but was: %s".formatted(
                            Event.Type.STARTED,
                            header.type()
                    ));
                }
                if (!state.compareAndSet(State.AWAITING_HANDSHAKE, State.HANDSHAKE_COMPLETED)) {
                    throw new IllegalStateException("Handshake failed! expect %s state, but was: %s".formatted(
                            State.AWAITING_HANDSHAKE,
                            state.get()
                    ));
                }
                handler.onOpen(emitter);
                logger.debug("{}/{} handshake completed.", this, emitter.id());
                yield CompletableFuture.completedStage(null);
            }

            /*
             * STEP3 - 握手结束，开始正式通信
             */
            case HANDSHAKE_COMPLETED: {

                // 会话结束
                if (header.type() == Event.Type.FINISHED) {
                    logger.debug("{}/{} session finished.", this, emitter.id());
                    yield handler.onData(event.payload())
                            .thenAccept(u -> emitter.close());
                }

                // 会话通讯
                else if (header.type() == Event.Type.GENERATED) {
                    yield handler.onData(event.payload());
                }

                // 其他类型的数据帧不应该被支持
                else {
                    throw new IllegalStateException("Unexpected event type: " + header.type());
                }

            }
        };

    }

    @Override
    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
        return handler.onBinary(buffer);
    }

    @Override
    public void onClosed(Throwable ex) {
        handler.onClosed(ex);
    }

    private static class SessionEmitter extends Realtime.DelegateEmitter<String> {

        private final Mode mode;

        public SessionEmitter(Mode mode, Realtime.Emitter<String> delegate) {
            super(delegate);
            this.mode = mode;
        }

        public CompletionStage<Void> start(Realtime.Session<?, ?> session) {
            final var sessionPayload = JacksonJsonUtils.toJson(session);
            final var command = Command.of(id(), mode, Command.Action.RUN, sessionPayload);
            return super.data(command.toJson());
        }

        public CompletionStage<Void> finish() {
            final var command = Command.of(id(), mode, Command.Action.FINISH, "{\"input\": {}}");
            return super.data(command.toJson());
        }

        @Override
        public CompletionStage<Void> data(String input) {
            final var command = Command.of(id(), mode, Command.Action.CONTINUE, input);
            return super.data(command.toJson());
        }

        @Override
        public CompletionStage<Void> closing() {
            return finish();
        }

    }

    private enum State {
        AWAITING_HANDSHAKE,
        HANDSHAKE_COMPLETED
    }

    // --- COMMAND 协议
    record Command(

            @JsonProperty("header")
            Command.Header header,

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
            Event.Header header,

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

        @JsonProperty("none")
        ONCE,

        @JsonProperty("in")
        IN,

        @JsonProperty("out")
        OUT,

        @JsonProperty("duplex")
        DUPLEX

    }

}
