package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.HashMapToolbox;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolUse;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolboxPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.indexer.HashMapToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.skill.SkillLoader;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.toolkit.ToolkitLoader;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.dashscope.DashscopeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class ReActAgentTestCase implements LoadingEnv {

    @Test
    public void test$skill() {

        final var skillLoader = SkillLoader.newBuilder()
                .directories(List.of(
                        Path.of("./skills")
                ))
                .build();

        final var toolkitLoader = new ToolkitLoader()
                .append(ToolUse.Mode.FIXED, List.of(
                        DashscopeToolkit.create()
                ));

        final var toolbox = HashMapToolbox.newBuilder()
                .indexer(HashMapToolIndexer.newBuilder()
                        .client(client)
                        .cacheFile(Path.of(".toolbox-index-cache.jsonl"))
                        .build())
                .build();

        toolbox.subscribe(skillLoader);
        toolbox.subscribe(toolkitLoader);

        final var toolboxPlugin = ToolboxPlugin.newBuilder()
                .toolbox(toolbox)
                .enableSearchTools(true)
                .build();

        final var agent = ReActAgent.newBuilder()
                .client(client)
                .plugins(plugins -> {
                    plugins.add(toolboxPlugin);
                    return plugins;
                })
                .build();

        final var sessionId = UUID.randomUUID().toString();
        final var inbound = Message.user("小蓝的数学成绩是多少?");
        final var outbound = agent.async(sessionId, inbound)
                .toCompletableFuture()
                .join();

        System.out.println(outbound.text());

    }

}
