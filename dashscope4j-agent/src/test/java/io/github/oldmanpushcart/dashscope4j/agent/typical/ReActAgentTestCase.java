package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.agent.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.SimpleToolboxPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolUse;
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

        try(final var agent = ReActAgent.newBuilder()
                .client(client)
                .model(ChatModel.QWEN_FLASH)
                .plugins(plugins -> {
                    plugins.add(SimpleToolboxPlugin.newBuilder()
                            .skill(ToolUse.Mode.DYNAMIC, Path.of("./skills"))
                            .toolkit(ToolUse.Mode.DYNAMIC, DashscopeToolkit.create())
                            .build());
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
