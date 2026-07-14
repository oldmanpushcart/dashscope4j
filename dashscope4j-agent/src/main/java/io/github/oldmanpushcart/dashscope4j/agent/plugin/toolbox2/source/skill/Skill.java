package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.skill;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static io.github.oldmanpushcart.dashscope4j.client.util.CheckUtils.require;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

/**
 * 技能
 *
 * @param home       主目录
 * @param header     头信息
 * @param body       正文
 * @param lastModifiedAt 修改时间
 */
public record Skill(Path home, Header header, String body, Instant lastModifiedAt) {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final Pattern FRONTMATTER_PATTERN = Pattern.compile(
            "^---\\s*$(.*?)^---\\s*$",
            Pattern.DOTALL | Pattern.MULTILINE
    );

    /**
     * 从技能目录加载 Skill
     *
     * @param path 技能主目录
     * @return Skill 实例
     * @throws IOException 加载失败
     */
    public static Skill of(Path path) throws IOException {

        requireNonNull(path, "Path must not be null!");

        // 检查SKILL目录是否合法
        final var home = path.toAbsolutePath().normalize();
        require(home, Files::exists, "%s not exist!".formatted(home));
        require(home, Files::isDirectory, "%s is not a directory!".formatted(home));

        // 检查SKILL结构是否合法
        final var skillMd = home.resolve("SKILL.md");
        require(skillMd, Files::exists, "%s not exist!".formatted(skillMd));

        // 读取SKILL.md并检查是否合法
        final var content = Files.readString(skillMd, UTF_8);
        final var matcher = FRONTMATTER_PATTERN.matcher(content);
        if (!matcher.find()) {
            throw new IOException("Invalid SKILL.md format: missing YAML frontmatter (must start and end with ---)");
        }

        // 解析内容兵组装为SKILL对象
        final var header = YAML_MAPPER.readValue(matcher.group(1), Header.class);
        final var body = content.substring(matcher.end()).trim();
        final var lastModifiedAt = Files.getLastModifiedTime(skillMd).toInstant();

        return new Skill(home, header, body, lastModifiedAt);
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
    @JsonIgnoreProperties(ignoreUnknown = true)
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
