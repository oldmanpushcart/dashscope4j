package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.skill;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 技能
 *
 * @param home   主目录
 * @param header 头信息
 * @param body   正文
 */
public record Skill(Path home, Header header, String body) {

    /**
     * 从技能目录加载 Skill
     *
     * @param home 技能主目录
     * @return Skill 实例
     * @throws IOException 加载失败
     */
    public static Skill of(Path home) throws IOException {
        return SkillParser.parse(home);
    }

    /**
     * 技能头信息
     *
     * @param name          名称
     * @param description   描述
     * @param license       许可
     * @param compatibility 兼容性
     * @param metadata      元数据
     * @param allowedTools  允许的工具
     */
    public record Header(
            String name,
            String description,
            String license,
            String compatibility,
            Map<String, String> metadata,
            List<String> allowedTools
    ) {
    }

}
