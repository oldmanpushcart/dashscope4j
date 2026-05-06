package io.github.oldmanpushcart.dashscope4j.agent.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.ToolLoader;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

/**
 * 工具使用信息
 * <p>
 * 封装工具在 {@link Toolbox} 中的完整使用上下文：
 * <ul>
 *   <li>{@code tool} - 工具本体，标识"他是谁"</li>
 *   <li>{@code loader} - 工具来源，标识"他从哪里来"</li>
 *   <li>{@code mode} - 使用模式，标识"他要到哪里去"（对 LLM 的可见性策略）</li>
 * </ul>
 * </p>
 *
 * @param mode   工具使用模式，决定工具是否对 LLM 默认可见（FIXED: 始终可见 / DYNAMIC: 按需加载）
 * @param tool   工具实例，提供实际的工具功能定义
 * @param loader 工具加载器，标识工具的来源
 */
public record ToolUse(Mode mode, Tool tool, ToolLoader loader) {

    /**
     * 使用模式
     */
    public enum Mode {

        /**
         * 固定模式
         * <p>
         * 工具始终注册在 LLM 的工具列表中，对 LLM 可见。
         * 适用于常用工具、核心工具。
         * </p>
         */
        FIXED,

        /**
         * 动态模式
         * <p>
         * 工具按需动态加载，不主动出现在 LLM 的工具列表中。
         * 适用于插件式工具或大量工具场景。
         * </p>
         */
        DYNAMIC

    }

}
