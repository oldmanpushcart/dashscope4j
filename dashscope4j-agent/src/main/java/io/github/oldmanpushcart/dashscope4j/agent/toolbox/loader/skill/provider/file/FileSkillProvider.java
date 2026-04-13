package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill.provider.file;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill.Skill;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill.provider.SkillProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 文件系统 Skill 提供者
 * <p>
 * 从指定路径加载单个 Skill 文件夹。
 * </p>
 * <p>
 * 使用示例：
 * <pre>{@code
 * var provider = new FileSkillProvider(Paths.get("./skills/weekly-report"));
 * }</pre>
 */
public class FileSkillProvider implements SkillProvider {

    private final Path skillDir;
    private final String _toString;

    public FileSkillProvider(Path skillDir) {
        this.skillDir = skillDir.normalize();
        this._toString = "dashscope4j-agent:/skill-provider/file=%s".formatted(this.skillDir);
    }

    @Override
    public String toString() {
        return _toString;
    }

    @Override
    public CompletionStage<List<Skill>> provide() {
        try {
            final var skill = FileSkill.valueOf(skillDir, this);
            return CompletableFuture.completedFuture(List.of(skill));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletionStage<String> signature() {
        try {

            // 策略：基于 SKILL.md 的最后修改时间
            final var skillMdPath = skillDir.resolve("SKILL.md");
            if (Files.exists(skillMdPath)) {
                final var modTime = Files.getLastModifiedTime(skillMdPath).toMillis();
                return CompletableFuture.completedFuture(String.valueOf(modTime));
            }

            // 降级：返回目录的修改时间
            final var dirModTime = Files.getLastModifiedTime(skillDir).toMillis();
            return CompletableFuture.completedFuture(String.valueOf(dirModTime));

        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 创建一个文件系统 Skill 提供者
     *
     * @param skillDir Skill 目录路径
     * @return FileSkillProvider 实例
     */
    public static FileSkillProvider ofPath(Path skillDir) {
        return new FileSkillProvider(skillDir);
    }

}
