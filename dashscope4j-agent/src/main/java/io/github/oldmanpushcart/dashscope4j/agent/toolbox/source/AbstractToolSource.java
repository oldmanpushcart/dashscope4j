package io.github.oldmanpushcart.dashscope4j.agent.toolbox.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractToolSource implements ToolSource {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String name;
    private final List<WeakReference<Listener>> listenerRefs = new ArrayList<>();

    public AbstractToolSource(String name) {
        this.name = Objects.requireNonNullElseGet(name, () -> "0x%02X".formatted(SEQUENCE.incrementAndGet() & 0xFF));
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public synchronized void addListener(Listener listener) {
        listenerRefs.add(new WeakReference<>(listener));
    }

    @Override
    public synchronized void removeListener(Listener listener) {
        listenerRefs.removeIf(ref -> {
            final var target = ref.get();
            return target == null || target == listener;
        });
    }

    @Override
    public synchronized void close() {
        final var listenerRefIt = listenerRefs.iterator();
        while (listenerRefIt.hasNext()) {
            final var listenerRef = listenerRefIt.next();
            listenerRefIt.remove();
            final var listener = listenerRef.get();
            if (listener == null) {
                listenerRefIt.remove();
                continue;
            }
            try {
                listener.onClosed();
            } catch (Throwable t) {
                logger.warn("{} fire closed occur error! listener={}", this, listener, t);
            }
        }
    }

    /**
     * 触发监听器
     */
    protected synchronized void fireChanged() {
        final var listenerRefIt = listenerRefs.iterator();
        while (listenerRefIt.hasNext()) {
            final var listenerRef = listenerRefIt.next();
            final var listener = listenerRef.get();
            if (listener == null) {
                listenerRefIt.remove();
                continue;
            }
            try {
                listener.onChanged();
            } catch (Throwable t) {
                logger.warn("{} fire changed occur error! listener={}", this, listener, t);
            }
        }
    }

}
