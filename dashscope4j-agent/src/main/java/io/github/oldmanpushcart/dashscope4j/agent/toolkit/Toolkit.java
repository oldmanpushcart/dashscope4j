package io.github.oldmanpushcart.dashscope4j.agent.toolkit;

import io.github.oldmanpushcart.dashscope4j.agent.toolkit.file.FileOpsToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.RuntimeToolkit;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.system.ShellToolkit;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.List;

/**
 * 工具包接口
 * <p>
 * 定义了一组相关工具的集合，用于向 LLM Agent 提供特定领域的能力。
 * 每个工具包可以包含多个功能相关的工具，例如：
 * </p>
 * <ul>
 *   <li>{@link FileOpsToolkit} - 文件操作工具包</li>
 *   <li>{@link ShellToolkit} - Shell 命令执行工具包</li>
 *   <li>{@link RuntimeToolkit} - 运行时环境信息查询工具包</li>
 * </ul>
 * <p>
 * <b>设计目的：</b>
 * </p>
 * <ul>
 *   <li>将相关工具组织在一起，便于管理和配置</li>
 *   <li>支持按需启用/禁用特定功能模块</li>
 *   <li>提供统一的工具注册接口</li>
 * </ul>
 *
 * @see Tool 单个工具接口
 * @see FunctionTool 函数型工具实现
 */
public interface Toolkit {

    /**
     * 获取此工具包提供的所有工具列表
     * <p>
     * 返回的工具列表将被注册到 LLM Agent 中，供其在需要时调用。
     * 每个工具都应该有清晰的名称、描述和参数定义。
     * </p>
     *
     * @return 不可变的工具列表，包含此工具包提供的所有可用工具
     */
    List<Tool> tools();

}
