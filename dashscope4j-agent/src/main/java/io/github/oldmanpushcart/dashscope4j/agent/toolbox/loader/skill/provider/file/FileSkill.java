package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill.provider.file;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill.Skill;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill.SkillHelper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 基于文件的 Skill 实现
 *
 * @since 4.0.0
 */
class FileSkill implements Skill {

    // 默认缓冲区大小
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    // 文件常量
    private static final String SKILL_MD_FILE = "SKILL.md";

    private final Path home;
    private final Header header;
    private final String body;
    private final String _toString;

    /**
     * 包级私有构造函数
     */
    FileSkill(Path home, Header header, String body) {
        this.home = home.toAbsolutePath().normalize();
        this.header = header;
        this.body = body;
        this._toString = "dashscope4j-agent:/skill/%s".formatted(header.name());
    }

    @Override
    public String toString() {
        return _toString;
    }

    /**
     * @return SKILL 主目录
     */
    public Path home() {
        return home;
    }

    @Override
    public Header header() {
        return header;
    }

    @Override
    public String body() {
        return body;
    }

    // === Reference 实现 ===

    @Override
    public CompletionStage<String> reference(String relativePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final var resourcePath = resolveAndValidate(relativePath);
                if (!Files.exists(resourcePath)) {
                    throw new IOException("Reference not found: " + relativePath);
                }
                return Files.readString(resourcePath, UTF_8);
            } catch (IOException e) {
                throw new CompletionException("Failed to read reference", e);
            }
        });
    }

    // === Asset 实现 ===

    @Override
    public void asset(String relativePath, AssetHandler handler) {
        CompletableFuture.runAsync(() -> {
            try {
                final var resourcePath = resolveAndValidate(relativePath);
                if (!Files.exists(resourcePath)) {
                    throw new IOException("Asset not found: " + relativePath);
                }

                // 分块读取并发送
                try (final var is = Files.newInputStream(resourcePath)) {
                    final var buffer = new byte[DEFAULT_BUFFER_SIZE];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, bytesRead);
                        handler.onRead(byteBuffer);
                    }
                }

                // 完成
                handler.onCompleted();
            } catch (IOException e) {
                handler.onFailure(e);
            }
        });
    }

    // === Script 实现 ===

    @Override
    public CompletionStage<String> script(String relativePath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final var resourcePath = resolveAndValidate(relativePath);
                if (!Files.exists(resourcePath)) {
                    throw new IOException("Script not found: " + relativePath);
                }
                return Files.readString(resourcePath, UTF_8);
            } catch (IOException e) {
                throw new CompletionException("Failed to read script", e);
            }
        });
    }

    @Override
    public int hashCode() {
        return Objects.hash(home, header, body);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof FileSkill otherSkill) {
            return Objects.equals(home, otherSkill.home())
                    && Objects.equals(header, otherSkill.header())
                    && Objects.equals(body, otherSkill.body());
        } else {
            return false;
        }
    }

    // === 辅助方法 ===

    /**
     * 解析并验证路径
     */
    private Path resolveAndValidate(String relativePath) {
        final var resolved = home.resolve(relativePath).normalize();

        // 安全检查：防止目录穿越
        if (!resolved.startsWith(home)) {
            throw new SecurityException("Path escapes skill directory: " + relativePath);
        }

        return resolved;
    }


    /**
     * 从目录加载 Skill
     *
     * @param skillDir Skill 目录路径
     * @return FileSkill 实例
     * @throws IOException 如果加载失败
     */
    public static FileSkill valueOf(Path skillDir) throws IOException {
        final var home = skillDir.toAbsolutePath().normalize();
        final var name = home.getFileName().toString();

        // 验证命名规范
        SkillHelper.validateName(name);

        // 读取并解析 SKILL.md
        final var skillMdFile = home.resolve(SKILL_MD_FILE);
        if (!Files.exists(skillMdFile)) {
            throw new IOException("SKILL.md not found in: " + home);
        }

        final var content = Files.readString(skillMdFile, UTF_8);
        final var parsed = SkillHelper.parse(content);

        return new FileSkill(home, parsed.header(), parsed.body());
    }

}
