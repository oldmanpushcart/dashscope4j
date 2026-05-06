package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Skill 解析器
 * 负责从技能目录解析 SKILL.md 文件并构建 Skill 对象
 */
class SkillParser {

    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile(
            "^---\\s*$(.*?)^---\\s*$",
            Pattern.DOTALL | Pattern.MULTILINE
    );

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private static final String SKILL_MD_FILE = "SKILL.md";

    /**
     * 从技能目录解析 Skill
     *
     * @param home 技能主目录
     * @return Skill 实例
     * @throws IOException 解析失败
     */
    public static Skill parse(Path home) throws IOException {
        // 验证目录
        final var skillDir = validateDirectory(home);

        // 读取 SKILL.md
        final var content = readSkillMd(skillDir);

        // 解析内容
        final var header = parseHeader(content);
        final var body = extractBody(content);

        return new Skill(skillDir, header, body);
    }

    /**
     * 验证技能目录
     */
    private static Path validateDirectory(Path path) throws IOException {
        final var home = path.toAbsolutePath().normalize();
        if (!Files.exists(home)) {
            throw new IOException("Skill directory not found: " + home);
        }
        if (!Files.isDirectory(home)) {
            throw new IOException("Path is not a directory: " + home);
        }
        return home;
    }

    /**
     * 读取 SKILL.md 文件内容
     */
    private static String readSkillMd(Path home) throws IOException {
        final var skillMdPath = home.resolve(SKILL_MD_FILE);
        if (!Files.exists(skillMdPath)) {
            throw new IOException("SKILL.md not found in: " + home);
        }
        return Files.readString(skillMdPath, UTF_8);
    }

    /**
     * 解析 Header
     */
    private static Skill.Header parseHeader(String content) throws IOException {
        final var matcher = FRONTMATTER_PATTERN.matcher(content);
        if (!matcher.find()) {
            throw new IOException("Invalid SKILL.md format: missing YAML frontmatter (must start and end with ---)");
        }

        final var yamlContent = matcher.group(1);
        final var rawHeader = YAML_MAPPER.readValue(yamlContent, YamlFrontmatter.class);

        // 处理 null 值，提供默认值
        return new Skill.Header(
                rawHeader.name(),
                rawHeader.description(),
                rawHeader.license(),
                rawHeader.compatibility(),
                Objects.requireNonNullElse(rawHeader.metadata(), Map.of()),
                Objects.requireNonNullElse(rawHeader.allowedTools(), List.of())
        );
    }

    /**
     * 提取正文部分
     */
    private static String extractBody(String content) {
        final var matcher = FRONTMATTER_PATTERN.matcher(content);
        if (matcher.find()) {
            return content.substring(matcher.end()).trim();
        }
        return content.trim();
    }

    /**
     * YAML Frontmatter 数据结构（仅用于解析）
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record YamlFrontmatter(
            String name,
            String description,
            String license,
            String compatibility,
            Map<String, String> metadata,
            List<String> allowedTools
    ) {
    }

}
