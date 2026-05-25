package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.skill;

import io.github.oldmanpushcart.dashscope4j.client.util.CheckUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 技能
 *
 * @param home   主目录
 * @param header 头信息
 * @param body   正文
 */
public record Skill(Path home, Header header, String body) {

    /**
     * 读取引用内容
     *
     * @param relativePath 引用相对路径
     * @return 引用内容
     * @throws IOException 读取错误
     */
    public String getReference(String relativePath) throws IOException {
        final var resourcePath = resolveAndValidate(relativePath);
        if (!Files.exists(resourcePath)) {
            throw new IOException("Reference not found: " + relativePath);
        }
        return Files.readString(resourcePath, UTF_8);
    }

    /**
     * 获取静态资源文件
     *
     * @param relativePath 静态资源相对路径
     * @return 静态资源文件
     * @throws IOException 文件不存在或路径非法
     */
    public Path getAsset(String relativePath) throws IOException {
        final var resourcePath = resolveAndValidate(relativePath);
        if (!Files.exists(resourcePath)) {
            throw new IOException("Asset not found: " + relativePath);
        }
        return resourcePath.toAbsolutePath();
    }


    /**
     * 执行脚本
     *
     * @param relativePath 脚本相对路径（用于验证和错误提示）
     * @param commands     完整的命令列表（如 ["python", "/path/to/script.py", "--arg1", "value1"]）
     * @param timeout      超时时间
     * @return 脚本执行结果（标准输出）
     * @throws IOException 脚本执行失败
     */
    public String executeScript(String relativePath, List<String> commands, Duration timeout) throws IOException {
        final var scriptPath = resolveAndValidate(relativePath);
        if (!Files.exists(scriptPath)) {
            throw new IOException("Script not found: " + relativePath);
        }

        // 启动进程
        final var process = new ProcessBuilder(commands)
                .directory(home.toFile())
                .redirectErrorStream(true)
                .start();

        try {

            // 等待进程完成，支持超时
            final var completed = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);

            if (!completed) {
                process.destroyForcibly();
                throw new IOException("Script execution timeout after %sms".formatted(timeout.toMillis()));
            }

            // 检查退出码
            final var exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IOException("Script exited with code: %s".formatted(exitCode));
            }

            // 读取标准输出并关闭流
            try (final var inputStream = process.getInputStream()) {
                return new String(inputStream.readAllBytes(), UTF_8).trim();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("Script execution interrupted", e);
        }
    }

    /**
     * 解析并验证路径（防止目录穿越攻击）
     *
     * @param relativePath 相对路径
     * @return 解析后的绝对路径
     * @throws SecurityException 路径非法或尝试穿越
     */
    private Path resolveAndValidate(String relativePath) {
        CheckUtils.requireNonBlankString(relativePath, "Relative path cannot be blank");

        // 解析路径
        final var resolved = home.resolve(relativePath).normalize();

        // 安全检查：防止目录穿越
        if (!resolved.startsWith(home)) {
            throw new SecurityException("Path escapes skill directory: %s".formatted(relativePath));
        }

        return resolved;
    }

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
