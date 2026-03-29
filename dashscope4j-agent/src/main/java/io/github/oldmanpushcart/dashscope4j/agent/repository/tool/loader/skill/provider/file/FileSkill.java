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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.regex.Matcher;
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
    private static final String REFERENCES_DIR = "references";
    private static final String ASSETS_DIR = "assets";
    private static final String SCRIPTS_DIR = "scripts";

    private final Path basePath;
    private final String name;
    private final String description;
    private final String license;
    private final String compatibility;
    private final Map<String, String> metadata;
    private final List<String> allowedTools;
    private final String bodyContent;

    public FileSkill(Path basePath) throws IOException {
        this.basePath = basePath.toAbsolutePath().normalize();

        // 解析 SKILL.md
        final var skillMdFile = basePath.resolve(SKILL_MD_FILE);
        if (!Files.exists(skillMdFile)) {
            throw new IOException("SKILL.md not found in: " + basePath);
        }

        final var content = Files.readString(skillMdFile, UTF_8);

        // 使用正则剥离 YAML frontmatter
        final var frontmatter = parseFrontmatter(content);

        this.name = validateName(frontmatter.name, basePath);
        this.description = frontmatter.description;
        this.license = frontmatter.license;
        this.compatibility = frontmatter.compatibility;
        this.metadata = convertMetadataMap(frontmatter.metadata);
        this.allowedTools = frontmatter.allowedTools != null ? List.copyOf(frontmatter.allowedTools) : List.of();
        this.bodyContent = extractBody(content);

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

    @Override
    public String bodyContent() {
        return bodyContent;
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

    // === 辅助方法 ===

    /**
     * 解析并验证路径
     */
    private Path resolveAndValidate(String relativePath) {
        Path resolved = basePath.resolve(relativePath).normalize();

        // 安全检查：防止目录穿越
        if (!resolved.startsWith(basePath)) {
            throw new SecurityException("Path escapes skill directory: " + relativePath);
        }

        return resolved;
    }

    /**
     * 验证 Skill 名称
     */
    private String validateName(String name, Path basePath) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }

        // 验证命名规范：小写 + 连字符，1-64 字符
        if (!name.matches("^[a-z0-9]+(-[a-z0-9]+)*$")) {
            throw new IllegalArgumentException("Invalid name format: " + name);
        }

        if (name.length() > 64) {
            throw new IllegalArgumentException("name too long: " + name);
        }

        // 检查是否与目录名一致
        String dirName = basePath.getFileName().toString();
        if (!name.equals(dirName)) {
            throw new IllegalArgumentException("name must match directory name: " + dirName);
        }

        return name;
    }

    /**
     * 解析 YAML frontmatter（使用正则 + Jackson）
     */
    private YamlFrontmatter parseFrontmatter(String content) throws IOException {

        // 使用正则匹配 frontmatter
        Matcher matcher = FRONTMATTER_PATTERN.matcher(content);

        if (!matcher.find()) {
            throw new IOException("Invalid SKILL.md format: missing YAML frontmatter (must start and end with ---)");
        }

        // 提取 YAML 内容
        String yamlContent = matcher.group(1);

        // 使用 Jackson 解析 YAML
        try {
            return YAML_MAPPER.readValue(yamlContent, YamlFrontmatter.class);
        } catch (IOException e) {
            throw new IOException("Failed to parse YAML frontmatter: " + e.getMessage(), e);
        }

    }

    /**
     * 提取 Markdowm 正文（使用正则）
     */
    private String extractBody(String content) {
        Matcher matcher = FRONTMATTER_PATTERN.matcher(content);
        if (!matcher.find()) {
            return content.trim();
        }

        // 返回 frontmatter 之后的内容
        return content.substring(matcher.end()).trim();
    }

    /**
     * 转换 metadata 为 Map<String, String>
     */
    private Map<String, String> convertMetadataMap(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        
        final var result = new HashMap<String, String>();
        metadata.forEach((key, value) -> {
            if (value != null) {
                result.put(key, value.toString());
            }
        });
        return Map.copyOf(result);
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
            Map<String, Object> metadata,

            @JsonProperty("allowed-tools")
            List<String> allowedTools

    ) {
    }

}
