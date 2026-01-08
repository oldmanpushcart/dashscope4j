package io.github.oldmanpushcart.dashscope4j.client.internal.util.flow;

import java.util.concurrent.atomic.AtomicLong;

class Quota {
    private final AtomicLong requested = new AtomicLong(0);
    private final AtomicLong emitted = new AtomicLong(0);

    private static void safetyAccumulateAndGet(AtomicLong atomicLong, long delta) {
        atomicLong.accumulateAndGet(delta, (cur, req) ->
                cur == Long.MAX_VALUE || cur + req < 0 ? Long.MAX_VALUE : cur + req);
    }

    public void requested(long delta) {
        safetyAccumulateAndGet(requested, delta);
    }


    public void emitted(long delta) {
        safetyAccumulateAndGet(emitted, delta);
    }

    public long available() {
        final var r = requested.get();
        final var e = emitted.get();
        return r == Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0, r - e);
    }

}
