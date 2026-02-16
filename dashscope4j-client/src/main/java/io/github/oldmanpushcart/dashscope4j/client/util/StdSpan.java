package io.github.oldmanpushcart.dashscope4j.client.util;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class StdSpan implements Tracer.Span {

    private final Tracer.Span root;
    private final Tracer.Span parent;
    private final String traceId;
    private final int spanId;
    private final String name;

    private final AtomicInteger indexer;

    private final Instant timestamp = Instant.now();
    private final Map<String, String> properties = new ConcurrentHashMap<>();
    private final AtomicReference<Status> statusRef = new AtomicReference<>(Status.PENDING);

    public StdSpan(Tracer.Span root, Tracer.Span parent, String traceId, int spanId, String name, AtomicInteger indexer) {
        this.root = root;
        this.parent = parent;
        this.traceId = traceId;
        this.spanId = spanId;
        this.name = name;
        this.indexer = indexer;
    }

    @Override
    public Tracer.Span root() {
        return root;
    }

    @Override
    public Tracer.Span parent() {
        return parent;
    }

    @Override
    public String traceId() {
        return traceId;
    }

    @Override
    public int spanId() {
        return spanId;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Map<String, String> properties() {
        return properties;
    }

    @Override
    public Instant timestamp() {
        return timestamp;
    }

    @Override
    public boolean isRoot() {
        return parent == null;
    }

    @Override
    public boolean isTerminated() {
        return isRoot()
                ? status() != Status.PENDING
                : root().status() != Status.PENDING;
    }

    @Override
    public Tracer.Span property(String key, String value) {
        properties.put(key, value);
        return this;
    }

    @Override
    public Tracer.Span status(Status status) {
        statusRef.compareAndSet(Status.PENDING, status);
        return this;
    }

    @Override
    public Tracer.Span success() {
        return status(Status.SUCCESS);
    }

    @Override
    public Tracer.Span failure() {
        return status(Status.FAILURE);
    }

    @Override
    public Tracer.Span failure(Throwable t) {
        return status(Status.FAILURE)
                .property("error", t.getMessage());
    }

    @Override
    public Status status() {
        return statusRef.get();
    }

    @Override
    public Tracer.Span newChild(String name) {
        final var childSpanId = indexer.getAndIncrement();
        return isRoot()
                ? new StdSpan(this, this, traceId, childSpanId, name, indexer)
                : new StdSpan(root, parent, traceId, childSpanId, name, indexer);
    }

}