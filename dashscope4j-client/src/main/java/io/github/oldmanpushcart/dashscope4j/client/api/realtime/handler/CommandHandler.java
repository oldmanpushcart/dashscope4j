package io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.api.Ret;
import io.github.oldmanpushcart.dashscope4j.client.api.realtime.Realtime;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.ToStringDeserializer;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.dashscope4j.client.api.realtime.handler.CommandHandler.Command.Action.*;
import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

public class CommandHandler implements Realtime.Handler<String, String> {

    private final Mode mode;
    private final Object session;
    private final Realtime.Handler<String, String> handler;
    private final AtomicReference<State> state = new AtomicReference<>(State.AWAITING_STARTED);

    private volatile Emitter emitter;

    public CommandHandler(Mode mode, Object session, Realtime.Handler<String, String> handler) {
        this.mode = mode;
        this.session = session;
        this.handler = handler;
    }

    @Override
    public void onOpen(Realtime.Emitter<String> delegate) {
        final var emitter = new Emitter(mode, delegate);
        emitter.start(session);
        this.emitter = emitter;
        handler.onOpen(emitter);
    }

    @Override
    public CompletionStage<Void> onData(String output) {
        final var event = JacksonJsonUtils.toObject(output, Event.class);
        if (!event.header.isSuccess()) {
            return CompletableFuture.failedStage(new CommandErrorException(event.header));
        }
        final var s = state.get();
        switch (s) {
            case AWAITING_STARTED -> {
                if (event.header().type() == Event.Type.STARTED) {
                    if (state.compareAndSet(s, State.STARTED)) {
                        handler.onOpen(emitter);
                        return CompletableFuture.completedStage(null);
                    } else {
                        return CompletableFuture.failedStage(new IllegalStateException("Change state failed, expect %s state, but was: %s".formatted(
                                s,
                                state.get()
                        )));
                    }
                } else {
                    return CompletableFuture.failedStage(new IllegalStateException("Expect %s event, but was: %s".formatted(
                            "task-started",
                            event.header().type()
                    )));
                }
            }
            case STARTED -> handler.onData(event.payload());
        }

        return handler.onData(event.payload());
    }

    @Override
    public CompletionStage<Void> onBinary(ByteBuffer buffer) {
        return handler.onBinary(buffer);
    }

    @Override
    public void onClosed(Throwable ex) {
        handler.onClosed(ex);
    }

    private enum State {
        AWAITING_STARTED,
        STARTED
    }

    private static class Emitter implements Realtime.Emitter<String> {

        private final Mode mode;
        private final Realtime.Emitter<String> delegate;

        private Emitter(Mode mode, Realtime.Emitter<String> delegate) {
            this.mode = mode;
            this.delegate = delegate;
        }

        private CompletionStage<Void> start(Object session) {
            final var payload = JacksonJsonUtils.toJson(session);
            final var command = new Command(
                    new Command.Header(genUUID22(), mode, RUN),
                    payload
            );
            final var commandJson = JacksonJsonUtils.toJson(command);
            return delegate.emit(commandJson);
        }

        private CompletionStage<Void> finish() {
            final var command = new Command(
                    new Command.Header(genUUID22(), mode, FINISH),
                    "{\"input\": {}}"
            );
            final var commandJson = JacksonJsonUtils.toJson(command);
            return delegate.emit(commandJson);
        }

        @Override
        public CompletionStage<Void> emit(String input) {
            final var command = new Command(
                    new Command.Header(genUUID22(), mode, CONTINUE),
                    input
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
            return finish();
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


    public record Command(

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

    }

    public record Event(

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

        @JsonProperty("none")
        ONCE,

        @JsonProperty("in")
        IN,

        @JsonProperty("out")
        OUT,

        @JsonProperty("duplex")
        DUPLEX

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
