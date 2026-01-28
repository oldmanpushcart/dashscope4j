package io.github.oldmanpushcart.dashscope4j.client.realtime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.Ret;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

public class CommandHandler<S, I, O> implements Realtime.Handler<String, String> {

    private final Mode mode;
    private final S session;
    private final Realtime.Handler<I, O> handler;

    public CommandHandler(Mode mode, S session, Realtime.Handler<I, O> handler) {
        this.mode = mode;
        this.session = session;
        this.handler = handler;
    }

    @Override
    public void onOpen(Realtime.Emitter<String> emitter) {
        final var commandEmitter = new Emitter<O>(mode, emitter);
        commandEmitter.start(session);
        handler.onOpen(commandEmitter);
    }

    @Override
    public CompletionStage<Void> onData(String input) {
        return null;
    }

    @Override
    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
        return handler.onBinary(buffer);
    }

    @Override
    public void onClosed(Throwable ex) {
        handler.onClosed(ex);
    }

    private static class Emitter<O> implements Realtime.Emitter<O> {

        private final Mode mode;
        private final Realtime.Emitter<String> delegate;

        private Emitter(Mode mode, Realtime.Emitter<String> delegate) {
            this.mode = mode;
            this.delegate = delegate;
        }

        private CompletionStage<Void> start(Object payload) {
            final var command = new Command<Object>(
                    new Command.Header(
                            genUUID22(),
                            mode,
                            Command.Action.RUN
                    ),
                    payload
            );
            final var commandJson = JacksonJsonUtils.toJson(command);
            return delegate.emit(commandJson);
        }

        private CompletionStage<Void> finish() {
            final var command = new Command<Object>(
                    new Command.Header(
                            genUUID22(),
                            mode,
                            Command.Action.FINISH
                    ),
                    new HashMap<>() {{
                        put("input", new Object());
                    }}
            );
            final var commandJson = JacksonJsonUtils.toJson(command);
            return delegate.emit(commandJson);
        }

        @Override
        public CompletionStage<Void> emit(O output) {
            final var command = new Command<O>(
                    new Command.Header(
                            genUUID22(),
                            mode,
                            Command.Action.CONTINUE
                    ),
                    output
            );
            final var commandJson = JacksonJsonUtils.toJson(command);
            return delegate.emit(commandJson);
        }

        @Override
        public CompletionStage<Void> emitBinary(ByteBuffer buffer) {
            return delegate.emitBinary(buffer);
        }

        @Override
        public CompletionStage<Void> emitClose() {
            return delegate.emitClose();
        }

        @Override
        public CompletionStage<Void> emitClose(Throwable ex) {
            return delegate.emitClose(ex);
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


    public record Command<T>(
            @JsonProperty("header") Header header,
            @JsonProperty("payload") T payload
    ) {

        record Header(
                @JsonProperty("task_id") String id,
                @JsonProperty("streaming") Mode mode,
                @JsonProperty("action") Action action
        ) {

        }

        public enum Action {
            @JsonProperty("run-task") RUN,
            @JsonProperty("continue-task") CONTINUE,
            @JsonProperty("finish-task") FINISH,
        }

    }

    public record Event<R>(
            @JsonProperty("header") Header header,
            @JsonProperty("payload") R payload
    ) {

        public static final class Header extends Ret {

            private final String id;
            private final Type type;

            @JsonCreator
            public Header(
                    @JsonProperty("task_id") String id,
                    @JsonProperty("event") Type type,
                    @JsonProperty("error_code") String code,
                    @JsonProperty("error_message") String desc
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
            @JsonProperty("task-started") STARTED,
            @JsonProperty("result-generated") GENERATED,
            @JsonProperty("task-finished") FINISHED,
            @JsonProperty("task-failed") FAILED
        }

    }

    public enum Mode {
        @JsonProperty("none") ONCE,
        @JsonProperty("in") IN,
        @JsonProperty("out") OUT,
        @JsonProperty("duplex") DUPLEX
    }

    public static class CommandErrorException extends RuntimeException {

        private final String code;
        private final String reason;

        public CommandErrorException(Ret ret) {
            super("command exchange occur error! code=%s;desc=%s".formatted(
                    ret.code(),
                    ret.desc()
            ));
            this.code = ret.code();
            this.reason = ret.desc();
        }

        public String code() {
            return code;
        }

        public String reason() {
            return reason;
        }

    }

}
