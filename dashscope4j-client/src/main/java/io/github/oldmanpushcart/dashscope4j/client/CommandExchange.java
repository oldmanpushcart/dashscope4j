package io.github.oldmanpushcart.dashscope4j.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.dashscope4j.common.util.UUIDUtils.genUUID22;

public class CommandExchange<T> implements Exchange<T> {

    private final Mode mode;
    private final Exchange<Command<?>> delegate;

    private CommandExchange(Mode mode, Exchange<Command<?>> delegate) {
        this.mode = mode;
        this.delegate = delegate;
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
    public CompletionStage<Void> closing() {
        return finish();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public CompletionStage<Void> closeFuture() {
        return delegate.closeFuture();
    }

    @Override
    public CompletionStage<Void> send(T data) {
        final var command = new Command<T>(
                new Command.Header(
                        genUUID22(),
                        mode,
                        Command.Action.CONTINUE
                ),
                data
        );
        return delegate.send(command);
    }

    @Override
    public CompletionStage<Void> send(ByteBuffer buffer) {
        return delegate.send(buffer);
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
        return delegate.send(command);
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
        return delegate.send(command);
    }

    public static class Handler<S, T, R> implements Exchange.Handler<Command<?>, Event<R>> {

        private final Mode mode;
        private final S session;
        private final Exchange.Handler<T, R> next;

        private final AtomicReference<State> state = new AtomicReference<>(State.AWAITING_STARTED);
        private volatile CommandExchange<T> exchange;

        public Handler(Mode mode, S session, Exchange.Handler<T, R> next) {
            this.mode = mode;
            this.next = next;
            this.session = session;
        }

        @Override
        public void onOpen(Exchange<Command<?>> delegate) {
            this.exchange = new CommandExchange<>(mode, delegate);
            exchange.start(session)
                    .thenApply(unused -> delegate);
        }

        @Override
        public CompletionStage<Void> onData(Event<R> data) {

            if (!data.header().isSuccess()) {
                return CompletableFuture.failedStage(new CommandErrorException(data.header()));
            }

            final var s = state.get();
            switch (s) {
                case AWAITING_STARTED -> {
                    if (data.header().type() == Event.Type.STARTED) {
                        if (state.compareAndSet(s, State.STARTED)) {
                            next.onOpen(exchange);
                        } else {
                            final var cause = new IllegalStateException("Change state failed, expect %s state, but was: %s".formatted(
                                    s,
                                    state.get()
                            ));
                            return CompletableFuture.failedStage(cause);
                        }
                    } else {
                        final var cause = new IllegalStateException("Expect %s event, but was: %s".formatted(
                                "task-started",
                                data.header().type()
                        ));
                        return CompletableFuture.failedStage(cause);
                    }
                }
                case STARTED -> next.onData(data.payload());
            }

            return next.onData(data.payload);
        }

        @Override
        public CompletionStage<Void> onBinary(ByteBuffer buffer) {
            return next.onBinary(buffer);
        }

        @Override
        public void onClosed(Throwable ex) {
            next.onClosed(ex);
        }

        private enum State {

            AWAITING_STARTED,
            STARTED

        }

    }




}
