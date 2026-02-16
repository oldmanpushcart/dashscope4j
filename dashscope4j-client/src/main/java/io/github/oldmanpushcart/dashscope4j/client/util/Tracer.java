package io.github.oldmanpushcart.dashscope4j.client.util;

import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class Tracer {

    private static final Logger logger = LoggerFactory.getLogger(Tracer.class);
    private static final ThreadLocal<Span> CONTEXT = new ThreadLocal<>();

    public interface Span {

        Span root();

        Span parent();

        String traceId();

        int spanId();

        String name();

        Map<String, String> properties();

        Instant timestamp();

        boolean isRoot();

        boolean isTerminated();

        Span property(String key, String value);

        Span status(Status status);

        Span success();

        Span failure();

        Span failure(Throwable t);

        Status status();

        Span newChild(String name);

        enum Status {
            PENDING,
            SUCCESS,
            FAILURE
        }

        static Span ofRoot(String name) {
            final var traceId = UUID.randomUUID().toString().replace("-", "");
            final var indexer = new AtomicInteger();
            final var spanId = indexer.getAndIncrement();
            return new StdSpan(null, null, traceId, spanId, name, indexer);
        }

    }

    public record Scope(Span span) implements AutoCloseable {

        public Span restore() {
            CONTEXT.set(span);
            return span;
        }

        @Override
        public void close() {
            log(span);
            if (span.isRoot()) {
                CONTEXT.remove();
            } else {
                CONTEXT.set(span.parent());
            }
        }

    }

    private static void log(Span span) {

        final var pid = span.isRoot()
                ? 0
                : span.parent().spanId();

        final var timeOffset = span.isRoot()
                ? Duration.between(span.timestamp(), Instant.now()).toMillis()
                : Duration.between(span.root().timestamp(), Instant.now()).toMillis();

        final var propertiesJson = JacksonJsonUtils.toJson(span.properties());

        // TID|PID|SID|STATUS|TIME-OFFSET|NAME:{...}
        logger.info("{}|{}|{}|{}|{}|{}:{}",
                span.traceId(),
                pid,
                span.spanId(),
                span.status(),
                timeOffset,
                span.name(),
                propertiesJson
        );
    }

    public static Scope enter(String name) {
        final var span = current()
                .filter(c -> !c.isTerminated())
                .map(c -> c.newChild(name))
                .orElseGet(() -> Span.ofRoot(name));

        log(span);
        final var scope = new Scope(span);
        scope.restore();
        return scope;
    }

    public static Optional<Span> current() {
        return Optional.ofNullable(CONTEXT.get());
    }

}
