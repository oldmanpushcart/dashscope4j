package io.github.oldmanpushcart.dashscope4j.agent.repository.tool;

import io.github.oldmanpushcart.dashscope4j.agent.repository.Repository;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具存储器
 */
public class ToolStorer implements Repository.Storer<String, Tool> {

    private final Map<String, Tool> sources = new ConcurrentHashMap<>();

    @Override
    public CompletionStage<Void> init() {
        return CompletableFuture.completedStage(null);
    }

    @Override
    public CompletionStage<Tool> get(String key) {
        return CompletableFuture.completedStage(sources.get(key));
    }

    @Override
    public CompletionStage<Void> upsert(String key, Tool item) {
        return CompletableFuture.completedStage(null)
                .thenAccept(unused -> sources.put(key, item));
    }

    @Override
    public CompletionStage<Void> remove(String key) {
        return CompletableFuture.completedStage(null)
                .thenAccept(unused -> sources.remove(key));
    }

    @Override
    public void close() {
        sources.clear();
    }

}
