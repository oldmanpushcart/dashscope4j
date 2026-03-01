package io.github.oldmanpushcart.dashscope4j.client.internal.util;

import java.util.concurrent.atomic.AtomicReference;

public class Fsm<S> {

    private final AtomicReference<S> state;

    public Fsm(S initial) {
        this.state = new AtomicReference<>(initial);
    }

    public void transition(S expected, S update, Runnable onTransition) {
        if (state.compareAndSet(expected, update)) {
            try {
                onTransition.run();
            } catch (RuntimeException rEx) {
                // rollback
                state.compareAndSet(update, expected);
                throw rEx;
            }
        }
    }

}
