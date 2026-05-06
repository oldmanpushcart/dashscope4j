package io.github.oldmanpushcart.dashscope4j.agent.toolbox3;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface ToolSubscriptionHandler {

    CompletionStage<Void> onSubscribe();

    void onChange(List<ToolUse> upserts, List<String> removes);

}
