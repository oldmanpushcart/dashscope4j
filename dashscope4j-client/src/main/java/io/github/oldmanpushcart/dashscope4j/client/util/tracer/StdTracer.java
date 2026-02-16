package io.github.oldmanpushcart.dashscope4j.client.util.tracer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class StdTracer implements Tracer {

    public static StdTracer INSTANCE = new StdTracer();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ThreadLocal<Tracer.Span> context = new ThreadLocal<>();
    private final Set<Listener> listeners = ConcurrentHashMap.newKeySet();

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
            fireListener(span);
            if (span.isRoot()) {
                context.remove();
            } else {
                context.set(span.parent());
            }
        }

    }

    private void fireListener(Tracer.Span span) {
        listeners.forEach(listener -> {
            try {
                listener.onSpan(span);
            } catch (Throwable ex) {
                logger.warn("dashscope4j-client://tracer fire listener occur error!", ex);
            }
        });
    }

    @Override
    public Tracer.Scope enter(String name) {
        final var span = current()
                .filter(c -> !c.isTerminated())
                .map(c -> c.newChild(name))
                .orElseGet(() -> Tracer.Span.ofRoot(name));

        final var scope = new ScopeImpl(span);
        scope.restore();

        fireListener(span);
        return scope;
    }

    @Override
    public Optional<Tracer.Span> current() {
        final var current = context.get();
        if (null == current || current.isTerminated()) {
            context.remove();
            return Optional.empty();
        }
        return Optional.of(current);
    }

    @Override
    public void registerListener(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterListener(Listener listener) {
        listeners.remove(listener);
    }

}
