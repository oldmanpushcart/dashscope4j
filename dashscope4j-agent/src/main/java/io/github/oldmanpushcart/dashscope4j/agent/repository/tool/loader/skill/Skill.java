package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill;

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

    // === 元数据信息 (来自 YAML frontmatter) ===

    /**
     * @return Skill 名称 (必须与目录名一致，1-64 字符，小写 + 连字符)
     */
    String name();

    /**
     * @return Skill 描述 (1-1024 字符，说明用途和使用场景)
     */
    String description();

    /**
     * @return 许可证信息 (可选)
     */
    default String license() {
        return null;
    }

    /**
     * @return 兼容性说明 (可选，如环境要求)
     */
    default String compatibility() {
        return null;
    }

    /**
     * @return 元数据映射 (可选，如 author/version 等)
     */
    default Map<String, String> metadata() {
        return Map.of();
    }

    /**
     * @return 允许使用的工具列表 (可选，空格分隔)
     */
    default List<String> allowedTools() {
        return List.of();
    }

    // === 内容访问 ===

    /**
     * @return SKILL.md 的正文内容 (Markdown 格式，不含 frontmatter)
     */
    String body();

    // === Reference 资源 (文档类) ===

    /**
     * 获取引用文档内容
     *
     * @param relativePath 相对路径 (如 "references/REFERENCE.md")
     * @return 文档内容的异步回调
     */
    CompletionStage<String> getReference(String relativePath);

    /**
     * 检查引用文档是否存在
     *
     * @param relativePath 相对路径
     * @return 是否存在
     */
    boolean hasReference(String relativePath);

    // === Assert 资源 (静态文件) ===

    /**
     * 打开静态资源通道
     *
     * @param relativePath 相对路径 (如 "assets/template.xlsx")
     * @param handler      接收资源的 Handler
     */
    void readAssert(String relativePath, ReadHandler handler);

    /**
     * 检查静态资源是否存在
     *
     * @param relativePath 相对路径
     * @return 是否存在
     */
    boolean hasAssert(String relativePath);

    // === Script 脚本 ===

    /**
     * 读取脚本内容
     *
     * @param scriptPath 脚本相对路径 (如 "scripts/extract.py")
     * @return 脚本内容的异步回调
     */
    CompletionStage<String> readScript(String scriptPath);

    /**
     * 检查脚本是否存在
     *
     * @param scriptPath 脚本相对路径
     * @return 是否存在
     */
    boolean hasScript(String scriptPath);

    // === Handler 接口 ===

    /**
     * 静态资源处理器 - 负责接收二进制数据
     */
    interface ReadHandler {

        void onRead(ByteBuffer buffer);

        void onFailure(IOException ex);

        void onCompleted();

    }

}
