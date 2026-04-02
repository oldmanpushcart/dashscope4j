package io.github.oldmanpushcart.dashscope4j.agent.tool.loader.skill;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;

import java.util.Map;

/**
 * 全局工具：获取引用文档
 */
class GetReferenceTool {

    public static final String TOOL_NAME = "global$skill$get_reference";

    private final Map<String, Skill> skills;

    public GetReferenceTool(Map<String, Skill> skills) {
        this.skills = skills;
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
                        - reference_path: 引用文件相对路径，相对于 skill 目录
                        
                        【路径使用严格规范】
                        ⚠️ 重要：路径参数是**资源的唯一标识符**,必须原样复制：
                        - 从 SKILL.md、README.md 等文档中看到的路径，必须**逐字复制**到 reference_path 参数
                        - 不要修改、优化或"修正"路径格式（例如：不要去掉前缀，不要转换为绝对路径）
                        - 不要对路径进行 URL 编码/解码
                        - 即使路径看起来"奇怪"或"不正确",也要原样保留
                        
                        ❌ 错误示例：
                        - 修改路径的前缀部分
                        - 将相对路径改为绝对路径
                        
                        ✅ 正确示例：
                        - 文档中怎么写，参数就怎么填，保持完全一致
                        
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

                    final var skill = skills.get(spec.name());
                    if (null == skill) {
                        throw new IllegalArgumentException("Unknown skill: %s. Available skills: %s".formatted(spec.name(), skills.keySet()));
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
