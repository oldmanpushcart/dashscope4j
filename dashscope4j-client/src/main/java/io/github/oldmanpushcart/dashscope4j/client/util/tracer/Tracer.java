package io.github.oldmanpushcart.dashscope4j.client.util.tracer;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public interface Tracer {

    Tracer instance = new StdTracer();

    Scope enter(String name);

    Optional<Span> current();

    interface Span {

        Span root();

        Span parent();

        String traceId();

        int spanId();

        String name();

        Map<String, String> properties();

        Instant timestamp();

        boolean isRoot();

        boolean isTerminated();

        Span self();

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

    interface Scope extends AutoCloseable {

        Span span();

        Span restore();

        @Override
        void close();

    }

}
