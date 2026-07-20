package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.agent.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.HashMapToolbox;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolboxPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.indexer.EmbeddingToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.source.skill.SkillToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.source.toolkit.ToolkitToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.dashscope.DashscopeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class ReActAgentTestCase implements LoadingEnv {

    @Test
    public void test$skill() {

        final var skillsTs = SkillToolSource.newBuilder()
                .home(Path.of("./skills/school-score"))
                .build()
                .initialize()
                .toCompletableFuture()
                .join();

        final var toolkitTs = ToolkitToolSource.newBuilder()
                .append(DashscopeToolkit.create())
                .build()
                .initialize()
                .toCompletableFuture()
                .join();

        final var toolbox = HashMapToolbox.newBuilder()
                .indexer(EmbeddingToolIndexer.newBuilder()
                        .client(client)
                        .storage(Path.of("./.embedding-tool-index-cache.jsonl"))
                        .build())
                .build();

        toolbox.subscribe(skillsTs)
                .toCompletableFuture()
                .join();

        toolbox.subscribe(toolkitTs)
                .toCompletableFuture()
                .join();

        try (skillsTs; toolkitTs; toolbox; final var agent = ReActAgent.newBuilder()
                .client(client)
                .model(ChatModel.QWEN_FLASH)
                .plugins(plugins -> {
                    final var toolboxPlugin = ToolboxPlugin.newBuilder()
                            .toolboxes(List.of(toolbox))
                            .build();
                    plugins.add(toolboxPlugin);
                    return plugins;
                })
                .build()) {

            final var sessionId = UUID.randomUUID().toString();
            final var inbound = Message.user("小蓝的数学成绩是多少?");
            final var outbound = agent.async(sessionId, inbound)
                    .toCompletableFuture()
                    .join();

            DashscopeAssertions.dashscopeAssertText(client, outbound.text(), "小蓝的数学成绩是 93 分。");

        }

    }

}
