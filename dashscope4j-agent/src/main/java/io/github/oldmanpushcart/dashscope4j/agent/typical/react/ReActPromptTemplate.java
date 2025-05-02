package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.agent.internal.util.JacksonUtils;
import io.github.oldmanpushcart.dashscope4j.agent.prompt.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunctionTool;

import java.util.List;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.agent.internal.util.IOUtils.resourceToString;

public class ReActPromptTemplate extends PromptTemplate {

    private static final String REACT_TEMPLATE_RES_NAME = "dashscope4j/agent/typical/react/react-prompt-template.md";

    public static final String NAME_TOOLS = "tools";
    public static final String NAME_TOOL_NAMES = "tool_names";
    public static final String NAME_QUESTION = "question";

    /**
     * 构造提示语模板
     *
     */
    public ReActPromptTemplate() {
        super(resourceToString(REACT_TEMPLATE_RES_NAME));
    }

    /**
     * 设置工具列表
     *
     * @param tools 工具列表
     * @return this
     */
    public ReActPromptTemplate tools(List<ChatFunctionTool> tools) {
        parameter(NAME_TOOLS, tools.stream()
                .map(ChatFunctionTool::meta)
                .map(JacksonUtils::toJson)
                .collect(Collectors.toList()));
        parameter(NAME_TOOL_NAMES, tools.stream()
                .map(ChatFunctionTool::meta)
                .map(ChatFunctionTool.Meta::name)
                .collect(Collectors.toList()));
        return this;
    }

    /**
     * 设置问题
     *
     * @param question 问题
     * @return this
     */
    public ReActPromptTemplate question(String question) {
        parameter(NAME_QUESTION, question);
        return this;
    }

}
