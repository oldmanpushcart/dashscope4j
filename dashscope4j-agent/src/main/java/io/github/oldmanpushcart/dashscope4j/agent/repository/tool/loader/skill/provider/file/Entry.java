package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill.provider.file;

import io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill.Skill;

import java.nio.file.Path;

/**
 * 技能条目 - 封装 Skill 和 Path
 *
 * @param skill 技能对象
 * @param path  技能目录路径
 */
record Entry(Skill skill, Path path) {
}
