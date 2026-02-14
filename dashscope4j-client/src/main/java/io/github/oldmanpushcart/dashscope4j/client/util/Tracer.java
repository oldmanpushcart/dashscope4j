package io.github.oldmanpushcart.dashscope4j.client.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Tracer {

    private static final ThreadLocal<Span> CONTEXT = new ThreadLocal<>();

    public static class Span {

        private static final int INITIAL_INDEX = 1000;

        private static String genId() {
            return UUID.randomUUID().toString().replace("-", "");
        }

        private final String name;
        private final String tid;
        private final Span parent;
        private final int sid;


        private final AtomicInteger indexer = new AtomicInteger(INITIAL_INDEX);
        private final Map<String, String> properties = new ConcurrentHashMap<>();
        private final AtomicReference<State> state = new AtomicReference<>(State.PENDING);

        private Span(String name, String tid, Span parent, int sid) {
            this.name = name;
            this.tid = tid;
            this.parent = parent;
            this.sid = sid;
        }

        public String name() {
            return name;
        }

        public String tid() {
            return tid;
        }

        public int pid() {
            return isRoot() ? INITIAL_INDEX : parent.sid();
        }

        public int sid() {
            return sid;
        }

        public Map<String, String> properties() {
            return properties;
        }

        public Span property(String name, String value) {
            properties.put(name, value);
            return this;
        }

        public boolean isRoot() {
            return parent == null;
        }

        public Span success() {
            state.compareAndSet(State.PENDING, State.SUCCESS);
            return this;
        }

        public Span failure() {
            state.compareAndSet(State.PENDING, State.FAILURE);
            return this;
        }

        Span newChild(String name) {
            return new Span(name, tid, this, indexer.getAndIncrement());
        }

        static Span ofRoot(String name) {
            return new Span(name, genId(), null, INITIAL_INDEX);
        }

    }

    public enum State {
        PENDING,
        SUCCESS,
        FAILURE
    }

    public record Scope(Span span) implements AutoCloseable {

        @Override
        public void close() {
            final var current = CONTEXT.get();
            if (span != current) {
                throw new IllegalStateException("Span is not the current span");
            }

            if (span.isRoot()) {
                CONTEXT.remove();
            } else {
                CONTEXT.set(span.parent);
            }
        }

        public Scope restore() {
            final var current = CONTEXT.get();
            if (current != null && span != current) {
                throw new IllegalStateException("Conflict span!");
            }
            CONTEXT.set(span);
            return this;
        }

    }

    public static Scope enter(String name) {
        final var current = CONTEXT.get();
        final var span = current == null
                ? Span.ofRoot(name)
                : current.newChild(name);
        CONTEXT.set(span);
        return new Scope(span);
    }

}
