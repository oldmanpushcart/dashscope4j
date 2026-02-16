package io.github.oldmanpushcart.dashscope4j.client.util.tracer;

import io.github.oldmanpushcart.dashscope4j.client.internal.util.jackson.JacksonJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

class StdTracer implements Tracer {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ThreadLocal<Tracer.Span> context = new ThreadLocal<>();

    private class ScopeImpl implements Tracer.Scope {

        private final Tracer.Span span;

        private ScopeImpl(Tracer.Span span) {
            this.span = span;
        }

        @Override
        public Tracer.Span span() {
            return span;
        }

        @Override
        public Tracer.Span restore() {
            context.set(span);
            return span;
        }

        @Override
        public void close() {
            log(span);
            if (span.isRoot()) {
                context.remove();
            } else {
                context.set(span.parent());
            }
        }

    }

    private void log(Tracer.Span span) {

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

    @Override
    public Tracer.Scope enter(String name) {
        final var span = current()
                .filter(c -> !c.isTerminated())
                .map(c -> c.newChild(name))
                .orElseGet(() -> Tracer.Span.ofRoot(name));

        log(span);
        final var scope = new ScopeImpl(span);
        scope.restore();
        return scope;
    }

    @Override
    public Optional<Tracer.Span> current() {
        return Optional.ofNullable(context.get());
    }

}
