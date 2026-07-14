package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.mcp;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;

/**
 * MCP 函数工具接口
 * <p>
 * 扩展自 FunctionTool，用于标识从 MCP 服务器加载的函数工具。
 * 支持三种类型的 MCP 资源：提示词（PROMPT）、工具（TOOL）、资源（RESOURCE）。
 * </p>
 *
 * @see FunctionTool
 */
public interface McpFunctionTool extends FunctionTool {

    /**
     * 获取 MCP 功能类型
     *
     * @return 功能类型（PROMPT/TOOL/RESOURCE）
     */
    Type type();

    /**
     * MCP 功能类型枚举
     * <p>
     * 定义了 MCP 协议支持的三种功能类型：
     * <ul>
     *     <li><b>PROMPT</b> - 提示词模板</li>
     *     <li><b>TOOL</b> - 可执行工具</li>
     *     <li><b>RESOURCE</b> - 数据资源</li>
     * </ul>
     * </p>
     */
    enum Type {
        /**
         * 提示词模板
         */
        PROMPT,
        
        /**
         * 可执行工具
         */
        TOOL,
        
        /**
         * 数据资源
         */
        RESOURCE
    }

}
