package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.agent.internal.util.JacksonUtils;
import io.github.oldmanpushcart.dashscope4j.agent.prompt.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunctionTool;

import java.util.List;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.agent.internal.util.ResourceUtils.resourceToString;

/**
 * React 提示语模板
 */
class ReActPromptTemplate extends PromptTemplate {

    private static final String PROMPT_RES_NAME = "dashscope4j/agent/prompt/react-prompt.md";

    public static final String NAME_TOOLS = "tools";
    public static final String NAME_TOOL_NAMES = "tool_names";
    public static final String NAME_QUESTION = "question";

    protected ReActPromptTemplate(Builder builder) {
        super(builder);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder extends PromptTemplate.Builder {

        public Builder() {
            template(resourceToString(PROMPT_RES_NAME));
        }

        public Builder(PromptTemplate template) {
            super(template);
        }

        /**
         * 设置工具列表
         *
         * @param tools 工具列表
         * @return this
         */
        public Builder tools(List<ChatFunctionTool> tools) {
            variable(NAME_TOOLS, tools.stream()
                    .map(ChatFunctionTool::meta)
                    .map(JacksonUtils::toJson)
                    .collect(Collectors.toList()));
            variable(NAME_TOOL_NAMES, tools.stream()
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
        public Builder question(String question) {
            variable(NAME_QUESTION, question);
            return this;
        }

    }

}
