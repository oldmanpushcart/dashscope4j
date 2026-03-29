package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;

import java.util.Map;

/**
 * 全局工具：获取引用文档
 */
class GetReferenceTool {

    public static final String TOOL_NAME = "global$skill$get_reference";

    private final Map<String, Skill> skillsMap;

    public GetReferenceTool(Map<String, Skill> skillsMap) {
        this.skillsMap = skillsMap;
    }

    public FunctionTool toTool() {
        return FunctionTool.newBuilder()
                .name(TOOL_NAME)
                .description("""
                        获取 Skill 中的引用文本内容。
                        
                        【使用场景】
                        需要查阅 Skill 提供的技术文档、参考资料、API 说明、模板文件等文本资源时使用此工具。
                        例如：查看函数的详细参数说明、阅读配置文件的格式要求、获取模板的使用说明等。
                        
                        【参数说明】
                        - skill_name: Skill 名称，如 "weekly-report-writer"
                        - reference_path: 引用文件相对路径，相对于 skill 目录，如 "references/REFERENCE.md" 或 "./template.md"
                        
                        【返回结果】
                        引用文件的完整文本内容，可直接用于阅读或作为其他工具的输入。
                        
                        【示例】
                        ```json
                        {
                          "skill_name": "code-analyzer",
                          "reference_path": "references/api-docs.md"
                        }
                        ```
                        """)
                .parameterType(Spec.class)
                .<Spec>function((caller, spec) -> {

                    final var skill = skillsMap.get(spec.name());
                    if (null == skill) {
                        throw new IllegalArgumentException("Unknown skill: %s. Available skills: %s".formatted(spec.name(), skillsMap.keySet()));
                    }

                    return skill.getReference(spec.path());
                })
                .build();
    }

    /**
     * 参数类
     */
    record Spec(
            @JsonProperty("skill_name")
            @JsonPropertyDescription("Skill 名称")
            String name,

            @JsonProperty("reference_path")
            @JsonPropertyDescription("引用文件相对路径，如 'references/REFERENCE.md'")
            String path
    ) {
    }

}
