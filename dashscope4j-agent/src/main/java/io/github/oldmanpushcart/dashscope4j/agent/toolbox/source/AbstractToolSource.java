package io.github.oldmanpushcart.dashscope4j.agent.toolbox.source;

import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractToolSource implements ToolSource {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String namespace;
    private final List<WeakReference<Listener>> listenerRefs = new ArrayList<>();

    public AbstractToolSource(String namespace) {
        this.namespace = CommonUtils.isBlankString(namespace) ? "default" : namespace;
    }

    @Override
    public String namespace() {
        return namespace;
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
