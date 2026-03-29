package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;

public class LoadSkillTool {

    private final Skill skill;

    public LoadSkillTool(Skill skill) {
        this.skill = skill;
    }

    public FunctionTool toTool() {
        final var toolName = SkillHelper.toToolName(skill.name());
        final var toolDescription = """
                %s
                
                【使用方法】
                调用此工具可以激活`%s`技能。激活后将获得该技能的完整 instructions 和指导。
                适用于所需场景。
                
                【参数说明】
                用户意图描述，说明想要完成的任务。Skill 会根据你的意图提供具体的执行步骤和指导。
                
                【返回结果】
                Skill 正文内容 (SKILL.md 的 instructions 部分) 和下一步行动引导。
                引导提示会告诉你如何使用该技能的其他功能 (如查阅资料、获取资源、执行脚本等)。
                
                【典型用途】
                - 启动一个专门的技能来处理特定任务
                - 获取某个领域的专业知识和操作指南
                - 按照标准化的流程完成复杂工作
                
                【示例】
                用户："我需要写一份周报"
                → 调用 skill$weekly-report-writer
                ← 获得周报写作技能的完整 instructions
                """.formatted(
                skill.description(),
                skill.name()
        );

        return FunctionTool.newBuilder()
                .name(toolName)
                .description(toolDescription)
                .parameterType(Spec.class)
                .function((caller, intent) -> """
                        %s
                        
                        ---
                        💡 引导提示：
                        
                        ✅ 已激活`%s`技能。
                        
                        请根据上述 instructions 执行任务。如果需要：
                        - 📖 查阅参考资料，使用 `global$skill$get_reference`
                        - 📁 获取资源文件，使用 `global$skill$get_assert`
                        - ⚙️ 执行脚本，使用 `global$skill$execute_script`
                        
                        现在请开始执行任务...
                        """.formatted(
                        skill.bodyContent(),
                        skill.name()
                ))
                .build();
    }

    /**
     * 用户意图参数 (用于 skill$<skill_name> 工具)
     */
    public record Spec(
            @JsonProperty("intent")
            @JsonPropertyDescription("用户意图描述，说明想要完成的任务")
            String intent
    ) {
    }
}
