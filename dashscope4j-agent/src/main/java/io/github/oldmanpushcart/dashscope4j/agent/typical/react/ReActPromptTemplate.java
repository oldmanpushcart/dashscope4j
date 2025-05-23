package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.agent.prompt.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.FunctionTool;

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
            = """
            Answer the following questions as best you can. You have access to the following tools:
            
            ${tools}
            
            Use the following format:
            
            Question: the input question you must answer
            Thought: you should always think about what to do
            Action: the action to take, should be one of ${tool_names}
            Action Input: the input to the action
            Observation: the result of the action
            ... (this Thought/Action/Action Input/Observation can be repeated zero or more times)
            Thought: I now know the final answer.
            Final Answer: the final answer to the original input question
            
            Please make sure that if you return JSON data, you return it in plain JSON format without using Markdown code blocks like ```json or anything similar.
            """;

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
        public Builder tools(List<FunctionTool> tools) {

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
                    .map(FunctionTool::meta)
                    .map(meta ->
                            PromptTemplate.newBuilder()
                                    .template("""
                                            ## ${name}
                                            ### SUMMARY
                                            ${summary}
                                            ### PARAMETER-SCHEMA
                                            ${parameter-schema}
                                            """)
                                    .variable("name", meta.name())
                                    .variable("summary", meta.description())
                                    .variable("parameter-schema", meta.parameterSchema())
                                    .build()
                                    .render())
                    .collect(Collectors.joining("\n")));

            /*
             * 工具名清单
             * ['工具1','工具2',...,'工具N']
             */
            variable(NAME_TOOL_NAMES, tools.stream()
                    .map(FunctionTool::meta)
                    .map(FunctionTool.Meta::name)
                    .collect(Collectors.toList()));

            return this;
        }

    }

}
