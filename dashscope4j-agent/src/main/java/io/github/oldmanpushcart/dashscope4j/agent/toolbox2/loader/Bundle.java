package io.github.oldmanpushcart.dashscope4j.agent.toolbox2.loader;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox2.ToolUse;

import java.util.List;

/**
 * 工具包，包含一组工具使用说明
 *
 * @param uses   工具使用说明列表
 * @param loader 工具加载器
 */
public record Bundle(List<ToolUse> uses, ToolLoader loader) {

}
