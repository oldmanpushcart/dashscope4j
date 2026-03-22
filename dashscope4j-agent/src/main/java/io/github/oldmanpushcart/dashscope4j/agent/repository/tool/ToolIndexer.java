package io.github.oldmanpushcart.dashscope4j.agent.repository.tool;

import io.github.oldmanpushcart.dashscope4j.agent.repository.Repository;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.Set;
import java.util.concurrent.CompletionStage;

public class ToolIndexer implements Repository.Indexer<String, Tool> {

    @Override
    public CompletionStage<Void> init() {
        return null;
    }

    @Override
    public CompletionStage<Set<String>> lookup(UserMessage instant) {
        return null;
    }

    @Override
    public CompletionStage<Void> upsert(String key, Tool item) {
        return null;
    }

    @Override
    public CompletionStage<Void> remove(String key) {
        return null;
    }

    @Override
    public void close() throws Exception {

    }

}
