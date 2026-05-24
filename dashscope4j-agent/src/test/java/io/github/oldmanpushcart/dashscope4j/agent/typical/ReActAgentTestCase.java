package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.HashMapToolbox;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.SimpleToolboxPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolUse;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolboxPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.indexer.HashMapToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.skill.SkillLoader;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.toolkit.ToolkitLoader;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.dashscope.DashscopeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.DashscopeAgent;
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

        final var agent = DashscopeAgent.newBuilder()
                .client(client)
                .model(ChatModel.QWEN_FLASH)
                .plugins(plugins -> {
                    plugins.add(SimpleToolboxPlugin.newBuilder()
                            .skill(ToolUse.Mode.DYNAMIC, Path.of("./skills"))
                            .toolkit(ToolUse.Mode.DYNAMIC, DashscopeToolkit.create())
                            .build());
                    return plugins;
                })
                .build();

        final var sessionId = UUID.randomUUID().toString();
        final var inbound = Message.user("小蓝的数学成绩是多少?");
        final var outbound = agent.async(sessionId, inbound)
                .toCompletableFuture()
                .join();

        System.out.println(outbound.text());
        agent.close();

    }

}
