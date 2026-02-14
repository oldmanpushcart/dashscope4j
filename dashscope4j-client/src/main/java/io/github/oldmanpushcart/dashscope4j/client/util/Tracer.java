package io.github.oldmanpushcart.dashscope4j.client.util;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class Tracer {

    private static final ThreadLocal<Span> CONTEXT = new ThreadLocal<>();

    private static String genId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public record Span(String name, String traceId, String spanId, Span parent, Instant beginAt) {

        public boolean isRoot() {
            return parent == null;
        }

        public Duration cost() {
            return Duration.between(beginAt, Instant.now());
        }

        public static Span ofRoot(String name) {
            return new Span(name, genId(), genId(), null, Instant.now());
        }

        public static Span ofChild(String name, Span parent) {
            return new Span(name, parent.traceId(), genId(), parent, Instant.now());
        }

    }

    public static Span current() {
        return CONTEXT.get();
    }

    public static Span enter(String name) {
        final var current = CONTEXT.get();
        final var span = current == null
                ? Span.ofRoot(name)
                : Span.ofChild(name, current);
        CONTEXT.set(span);
        return span;
    }

    public static void exit(Span span) {
        if (span.isRoot()) {
            CONTEXT.remove();
        } else {
            CONTEXT.set(span.parent());
        }
    }

}
