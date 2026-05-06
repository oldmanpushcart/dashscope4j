package io.github.oldmanpushcart.dashscope4j.agent.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.ToolLoader;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

/**
 * 工具使用说明
 *
 * @param mode   使用模式
 * @param tool   工具实例
 * @param loader 工具来源
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
