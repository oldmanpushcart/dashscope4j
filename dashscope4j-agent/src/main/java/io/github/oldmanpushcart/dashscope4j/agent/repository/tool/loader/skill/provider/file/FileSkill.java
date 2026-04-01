package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill.provider.file;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill.Skill;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * 基于文件的 Skill 实现
 *
 * @since 4.0.0
 */
public class FileSkill implements Skill {

    // YAML Frontmatter 正则表达式
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile(
            "^---\\s*$(.*?)^---\\s*$",
            Pattern.DOTALL | Pattern.MULTILINE
    );

    // Jackson ObjectMapper (线程安全，可复用)
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    // 默认缓冲区大小
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    // 文件常量
    private static final String SKILL_MD_FILE = "SKILL.md";

    private final Path home;
    private final String name;
    private final String body;
    private final String description;
    private final String license;
    private final String compatibility;
    private final Map<String, String> metadata;
    private final List<String> allowedTools;
    private final String _toString;

    /**
     * 包级私有构造函数，通过 valueOf 或同包下直接创建实例
     */
    FileSkill(
            Path home, String name, String body,
            String description, String license, String compatibility, Map<String, String> metadata, List<String> allowedTools
    ) {
        this.home = home.toAbsolutePath().normalize();
        this.name = name;
        this.description = description;
        this.license = license;
        this.compatibility = compatibility;
        this.metadata = metadata;
        this.allowedTools = allowedTools;
        this.body = body;
        this._toString = "dashscope4j-agent:/skill/%s".formatted(name);
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
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public String body() {
        return body;
    }

    @Override
    public String license() {
        return license;
    }

    @Override
    public String compatibility() {
        return compatibility;
    }

    @Override
    public Map<String, String> metadata() {
        return metadata;
    }

    @Override
    public List<String> allowedTools() {
        return allowedTools;
    }

    // === Reference 实现 ===

    @Override
    public CompletionStage<String> getReference(String relativePath) {
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

    @Override
    public boolean hasReference(String relativePath) {
        try {
            Path resourcePath = resolveAndValidate(relativePath);
            return Files.exists(resourcePath);
        } catch (SecurityException e) {
            return false;
        }
    }

    // === Assert 实现 ===

    @Override
    public void readAssert(String relativePath, ReadHandler handler) {
        CompletableFuture.runAsync(() -> {
            try {
                Path resourcePath = resolveAndValidate(relativePath);
                if (!Files.exists(resourcePath)) {
                    throw new IOException("Assert not found: " + relativePath);
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

    @Override
    public boolean hasAssert(String relativePath) {
        try {
            Path resourcePath = resolveAndValidate(relativePath);
            return Files.exists(resourcePath);
        } catch (SecurityException e) {
            return false;
        }
    }

    // === Script 实现 ===

    @Override
    public CompletionStage<String> readScript(String scriptPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path scriptFile = resolveAndValidate(scriptPath);
                if (!Files.exists(scriptFile)) {
                    throw new IOException("Script not found: " + scriptPath);
                }
                return Files.readString(scriptFile, UTF_8);
            } catch (IOException e) {
                throw new CompletionException("Failed to read script", e);
            }
        });
    }

    @Override
    public boolean hasScript(String scriptPath) {
        try {
            Path scriptFile = resolveAndValidate(scriptPath);
            return Files.exists(scriptFile);
        } catch (SecurityException e) {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(home, name, description, license, compatibility, metadata, allowedTools);
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof FileSkill otherSkill) {
            return Objects.equals(home, otherSkill.home())
                    && Objects.equals(name, otherSkill.name())
                    && Objects.equals(description, otherSkill.description())
                    && Objects.equals(license, otherSkill.license())
                    && Objects.equals(compatibility, otherSkill.compatibility())
                    && Objects.equals(metadata, otherSkill.metadata())
                    && Objects.equals(allowedTools, otherSkill.allowedTools());
        } else {
            return false;
        }
    }

    // === 辅助方法 ===

    /**
     * 解析并验证路径
     */
    private Path resolveAndValidate(String relativePath) {
        Path resolved = home.resolve(relativePath).normalize();

        // 安全检查：防止目录穿越
        if (!resolved.startsWith(home)) {
            throw new SecurityException("Path escapes skill directory: " + relativePath);
        }

        return resolved;
    }


    /**
     * 转换 metadata 为 Map<String, String>
     */
    private static Map<String, String> convertMetadataMapStatic(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }

        return Map.copyOf(metadata);
    }


    /**
     * YAML Frontmatter 数据结构（使用 Jackson 注解）
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record YamlFrontmatter(

            @JsonProperty("name")
            String name,

            @JsonProperty("description")
            String description,

            @JsonProperty("license")
            String license,

            @JsonProperty("compatibility")
            String compatibility,

            @JsonProperty("metadata")
            Map<String, String> metadata,

            @JsonProperty("allowed-tools")
            List<String> allowedTools

    ) {
    }

    /**
     * 从目录加载 Skill
     *
     * @param skillDir Skill 目录路径
     * @return FileSkill 实例
     * @throws IOException 如果加载失败
     */
    public static FileSkill valueOf(Path skillDir) throws IOException {

        // SKILL 的 HOME 路径
        final var home = skillDir.toAbsolutePath().normalize();

        // SKILL 名称
        final var name = home.getFileName().toString();

        // 验证命名规范：小写 + 连字符
        if (!name.matches("^[a-z0-9]+(-[a-z0-9]+)*$")) {
            throw new IllegalArgumentException("Invalid name format: " + name);
        }

        // 验证命名规范：1-64 字符
        if (name.length() > 64) {
            throw new IllegalArgumentException("name too long: " + name);
        }

        /*
         * 解析 SKILL.md
         * ${HOME}/SKILL.md
         */
        final var skillMdFile = home.resolve(SKILL_MD_FILE);
        if (!Files.exists(skillMdFile)) {
            throw new IOException("SKILL.md not found in: " + home);
        }

        final var content = Files.readString(skillMdFile, UTF_8);

        // 使用正则匹配 frontmatter
        final var matcher = FRONTMATTER_PATTERN.matcher(content);

        if (!matcher.find()) {
            throw new IOException("Invalid SKILL.md format: missing YAML frontmatter (must start and end with ---)");
        }

        // 提取 YAML 内容
        final var yamlContent = matcher.group(1);

        // 使用 Jackson 解析 YAML
        final YamlFrontmatter frontmatter;
        try {
            frontmatter = YAML_MAPPER.readValue(yamlContent, YamlFrontmatter.class);
        } catch (IOException e) {
            throw new IOException("Failed to parse YAML frontmatter: " + e.getMessage(), e);
        }

        // 检查 SKILL 目录名是否和配置一样
        if (!name.equals(frontmatter.name())) {
            throw new IllegalArgumentException("YAML define name: %s but require: %s".formatted(
                    frontmatter.name,
                    name
            ));
        }

        // 提取 Markdown 正文
        final var body = content.substring(matcher.end()).trim();

        return new FileSkill(
                home,
                name,
                body,
                frontmatter.description(),
                frontmatter.license(),
                frontmatter.compatibility(),
                frontmatter.metadata(),
                frontmatter.allowedTools()
        );
    }

}
