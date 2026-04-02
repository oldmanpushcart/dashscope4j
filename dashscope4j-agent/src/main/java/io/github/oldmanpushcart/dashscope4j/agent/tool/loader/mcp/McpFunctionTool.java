package io.github.oldmanpushcart.dashscope4j.agent.tool.loader.mcp;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;

public interface McpFunctionTool extends FunctionTool {

    Type type();

    enum Type {
        PROMPT,
        TOOL,
        RESOURCE
    }

}
