package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill.provider.SkillProvider;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

/**
 * Skill 接口 - 定义 Skill 的元数据和资源访问能力
 * 符合 Anthropic Skills 规范 (<a href="https://agentskills.io/specification">...</a>)
 *
 * @since 4.0.0
 */
public interface Skill {

    /**
     * Skill Header - YAML frontmatter 的结构化表示
     */
    interface Header {
        String name();

        String description();

        String license();

        String compatibility();

        Map<String, String> metadata();

        List<String> allowedTools();
    }

    // === 核心内容访问 ===

    /**
     * @return Skill Header (YAML frontmatter)
     */
    Header header();

    /**
     * @return SKILL.md 的正文内容 (Markdown 格式，不含 frontmatter)
     */
    String body();

    /**
     * @return 提供此 Skill 的 Provider
     */
    SkillProvider from();

    // === Reference 资源 (文档类) ===

    /**
     * 引用文档内容
     *
     * @param relativePath 相对路径 (如 "references/REFERENCE.md")
     * @return 文档内容的异步回调
     */
    CompletionStage<String> reference(String relativePath);

    // === Asset 资源 (静态文件) ===

    /**
     * 打开静态资源通道
     *
     * @param relativePath 相对路径 (如 "assets/template.xlsx")
     * @param handler      接收资源的 Handler
     */
    void asset(String relativePath, AssetHandler handler);

    // === Script 脚本 ===

    /**
     * 读取脚本内容
     *
     * @param relativePath 脚本相对路径 (如 "scripts/extract.py")
     * @return 脚本内容的异步回调
     */
    CompletionStage<String> script(String relativePath);

    // === Handler 接口 ===

    /**
     * 静态资源处理器 - 负责接收二进制数据
     */
    interface AssetHandler {

        void onRead(ByteBuffer buffer);

        void onFailure(IOException ex);

        void onCompleted();

    }

}
