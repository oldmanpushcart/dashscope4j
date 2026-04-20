package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;

class LoadSkillTool {

    private final Skill skill;

    public LoadSkillTool(Skill skill) {
        this.skill = skill;
    }

    public FunctionTool toTool() {
        final var toolName = SkillHelper.toToolName(skill.header().name());
        final var toolDescription = """
                > 技能名：%s
                
                %s
                
                ## 激活说明
                - 调用此工具将激活技能，并获取其完整的操作指南与指令。
                - 技能的主说明是SKILL.md
                - **技能会被封装为一个工具，以`skill$`开头，请不要将工具名作为技能名传入！**
                
                
                ## 资源调用规范（至关重要）
                激活技能后，所有相关资源的访问必须且只能通过以下三个专用工具完成：
                
                ### 查阅引用文档 (如模板、MD文件):
                - 工具: global$skill$get_reference
                - 参数: skill_name (技能名), reference_path (文档相对路径)
                
                ### 获取静态资源 (如图片、数据文件):
                - 工具: global$skill$get_asset
                - 参数: skill_name (技能名), asset_path (资源相对路径)
                
                ### 执行脚本文件:
                - 工具: global$skill$execute_script
                - 参数: skill_name (技能名), script_path (脚本相对路径), args (脚本参数)
                
                ## 路径使用规范
                所有路径参数均为相对路径标识符，请务必遵守以下规则：
                - 原样使用：路径必须按照文档中给出的格式，一字不差地使用。
                - 禁止修改：不要尝试对路径进行任何形式的修改、优化、修正、编码或解码。即使路径看起来不符合常规，也必须原样保留。
                - 禁止使用其他工具：严禁使用此规范之外的任何工具（如通用文件读写、网络搜索等）来访问本技能的资源。
                
                ## 使用方法
                请向模型描述您希望完成的任务。模型将激活相应技能，并根据您的意图提供具体的执行步骤和指导。
                """.formatted(
                skill.header().name(),
                skill.header().description()
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
                        - 📁 获取资源文件，使用 `global$skill$get_asset`
                        - ⚙️ 执行脚本，使用 `global$skill$execute_script`
                        
                        现在请开始执行任务...
                        """.formatted(
                        skill.body(),
                        skill.header().name()
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
