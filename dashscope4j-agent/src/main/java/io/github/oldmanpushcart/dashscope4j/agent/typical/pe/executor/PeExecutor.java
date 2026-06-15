package io.github.oldmanpushcart.dashscope4j.agent.typical.pe.executor;

import java.util.concurrent.CompletionStage;

public interface PeExecutor<T, R> extends AutoCloseable {

    CompletionStage<R> async(String sessionId, T inbound);

    @Override
    void close();

}
