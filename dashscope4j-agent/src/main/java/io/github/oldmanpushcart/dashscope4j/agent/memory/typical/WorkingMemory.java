package io.github.oldmanpushcart.dashscope4j.agent.memory.typical;

import io.github.oldmanpushcart.dashscope4j.agent.memory.Memory;
import io.github.oldmanpushcart.dashscope4j.agent.memory.store.MemoryStore;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 工作内存
 */
public class WorkingMemory implements Memory {

    private final MemoryStore store;

    public WorkingMemory(MemoryStore store) {
        this.store = store;
    }

    @Override
    public CompletionStage<List<Message>> recall(String sessionId, UserMessage instant) {
        return null;
    }

    @Override
    public CompletionStage<Void> remember(String sessionId, Message inbound, Message outbound) {
        return store.upsert(sessionId, inbound, outbound)
                .thenApply(u -> null);
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(store);
    }

}
