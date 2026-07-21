package io.github.oldmanpushcart.dashscope4j.agent.toolbox.source;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface ToolSource extends AutoCloseable {

    String name();

    void addListener(Listener listener);

    void removeListener(Listener listener);

    List<Tool> tools();

    CompletionStage<? extends ToolSource> initialize();

    boolean isClosed();

    @Override
    void close();

    interface Listener {

        void onChanged();

        void onClosed();

    }

}
