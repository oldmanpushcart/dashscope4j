package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.agent.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.agent.hook.toolbox.ToolboxHook;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.HashMapToolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.indexer.EmbeddingToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.skill.SkillToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.dashscope.DashscopeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.UUID;

public class ReActAgentTestCase implements LoadingEnv {

    @Test
    public void test$skill() {

        final var toolbox = HashMapToolbox.newBuilder()
                .indexer(EmbeddingToolIndexer.newBuilder()
                        .client(client)
                        .storage(Path.of("./.embedding-tool-index-cache.jsonl"))
                        .build())
                .build();

        toolbox.subscribeSkill("dashscope4j", Path.of("./skills/school-score"))
                .toCompletableFuture()
                .join();

        toolbox.subscribeTools("dashscope4j", DashscopeToolkit.create())
                .toCompletableFuture()
                .join();

        try (toolbox) {

            final var agent = ReActAgent.newBuilder()
                    .client(client)
                    .model(ChatModel.QWEN_FLASH)
                    .addHook(ToolboxHook.newBuilder()
                            .toolbox(toolbox)
                            .build())
                    .build();

            final var sessionId = UUID.randomUUID().toString();
            final var inbound = Message.user("小蓝的数学成绩是多少?");
            final var outbound = agent.async(sessionId, inbound)
                    .toCompletableFuture()
                    .join();

            DashscopeAssertions.dashscopeAssertText(client, outbound.text(), "小蓝的数学成绩是 93 分。");

        }

    }

}
