package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.agent.prompt.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.dashscope4j.util.JsonUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * React 提示语模板
 */
class ReActPromptTemplate extends PromptTemplate {

    /**
     * ReAct 提示语模板
     */
    private static final String REACT_PROMPT_TEMPLATE
            = "Answer the following questions as best you can. You have access to the following tools:\n" +
              "\n" +
              "${tools}\n" +
              "\n" +
              "Use the following format:\n" +
              "\n" +
              "Question: the input question you must answer\n" +
              "Thought: you should always think about what to do\n" +
              "Action: the action to take, should be one of ${tool_names}\n" +
              "Action Input: the input to the action\n" +
              "Observation: the result of the action\n" +
              "... (this Thought/Action/Action Input/Observation can be repeated zero or more times)\n" +
              "Thought: I now know the final answer.\n" +
              "Final Answer: the final answer to the original input question\n" +
              "\n" +
              "Please make sure that if you return JSON data, you return it in plain JSON format without using Markdown code blocks like ```json or anything similar.\n" +
              "\n" +
              "Question:\n" +
              "${question}";

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
            template(REACT_PROMPT_TEMPLATE);
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

            /*
             * 列出所有注册到智能体的工具
             * 格式为
             *
             * ### 工具名
             *
             * #### SUMMARY
             * 工具描述
             *
             * #### PARAMETER-SCHEMA
             * 参数定义（JSON-SCHEMA）
             */
            variable(NAME_TOOLS, tools.stream()
                    .map(ChatFunctionTool::meta)
                    .map(JsonUtils::toJson)
                    .collect(Collectors.toList()));

            /*
             * 工具名清单
             * ['工具1','工具2',...,'工具N']
             */
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
