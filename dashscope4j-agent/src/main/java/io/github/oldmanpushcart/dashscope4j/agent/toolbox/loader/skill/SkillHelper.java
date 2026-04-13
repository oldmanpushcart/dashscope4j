package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class SkillHelper {

    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile(
            "^---\\s*$(.*?)^---\\s*$",
            Pattern.DOTALL | Pattern.MULTILINE
    );

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    public static String toToolName(String skillName) {
        return "skill$" + skillName;
    }

    /**
     * 验证 Skill 命名规范
     *
     * @param name Skill 名称
     * @throws IllegalArgumentException 如果命名不符合规范
     */
    public static void validateName(String name) {
        if (!name.matches("^[a-z0-9]+(-[a-z0-9]+)*$")) {
            throw new IllegalArgumentException("Invalid name format: " + name);
        }
        if (name.length() > 64) {
            throw new IllegalArgumentException("name too long: " + name);
        }
    }

    /**
     * 解析 Skill Markdown 内容
     *
     * @param content SKILL.md 完整内容
     * @return 解析结果（Header + body）
     * @throws IOException 解析失败
     */
    public static Parsed parse(String content) throws IOException {
        var matcher = FRONTMATTER_PATTERN.matcher(content);

        if (!matcher.find()) {
            throw new IOException("Invalid SKILL.md format: missing YAML frontmatter (must start and end with ---)");
        }

        var yamlContent = matcher.group(1);
        YamlFrontmatter frontmatter;
        try {
            frontmatter = YAML_MAPPER.readValue(yamlContent, YamlFrontmatter.class);
        } catch (IOException e) {
            throw new IOException("Failed to parse YAML frontmatter: " + e.getMessage(), e);
        }

        var body = content.substring(matcher.end()).trim();

        return new Parsed(
                new HeaderImpl(
                        frontmatter.name,
                        frontmatter.description,
                        frontmatter.license,
                        frontmatter.compatibility,
                        frontmatter.metadata != null ? frontmatter.metadata : Map.of(),
                        frontmatter.allowedTools != null ? frontmatter.allowedTools : List.of()
                ),
                body
        );
    }

    /**
     * 解析后的 Skill 数据
     */
    public record Parsed(Skill.Header header, String body) {
    }

    /**
     * Header 实现类
     */
    private record HeaderImpl(
            String name,
            String description,
            String license,
            String compatibility,
            Map<String, String> metadata,
            List<String> allowedTools
    ) implements Skill.Header {
    }

    /**
     * YAML Frontmatter 数据结构
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record YamlFrontmatter(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("license") String license,
            @JsonProperty("compatibility") String compatibility,
            @JsonProperty("metadata") Map<String, String> metadata,
            @JsonProperty("allowed-tools") List<String> allowedTools
    ) {
    }

}
