package io.github.oldmanpushcart.dashscope4j.agent.tool;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.List;

public interface ToolKit {

    List<Tool> tools();

}
