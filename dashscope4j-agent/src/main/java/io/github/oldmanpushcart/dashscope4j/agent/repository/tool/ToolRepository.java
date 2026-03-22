package io.github.oldmanpushcart.dashscope4j.agent.repository.tool;

import io.github.oldmanpushcart.dashscope4j.agent.repository.BaseRepository;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.concurrent.CompletionStage;

public class ToolRepository extends BaseRepository<String, Tool> {

    protected ToolRepository(String name, Indexer<String, Tool> indexer, Storer<String, Tool> storer, Loader<String, Tool> loader) {
        super(name, indexer, storer, loader);
    }

}
