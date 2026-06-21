package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.skill;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolUse;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.AbstractToolLoader;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.nio.file.Path;
import java.util.List;

public class NewSkillsLoader extends AbstractToolLoader {

    @Override
    public List<ToolUse> loaded() {
        return List.of();
    }

    public static class Builder implements Buildable<NewSkillsLoader, Builder> {

        @Override
        public NewSkillsLoader build() {
            return new NewSkillsLoader();
        }

    }

}
