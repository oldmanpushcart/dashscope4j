package io.github.oldmanpushcart.dashscope4j.agent.tool.loader.skill;

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
                
                【技能说明】
                调用此工具可以激活 `%s` 技能。激活后将获得该技能的完整 instructions 和指导。
                
                【重要提示 - 资源访问规范】
                本技能相关的所有资源访问必须使用以下三个工具：
                - 📖 查阅引用文档 (模板、MD 文件等): 使用 `global$skill$get_reference`,指定 skill_name=技能名、reference_path=引用文档相对路径
                - 📁 获取静态资源 (图片、数据文件等): 使用 `global$skill$get_assert`,指定 skill_name=技能名、assert_path=静态资源相对路径
                - ⚙️ 执行脚本文件：使用 `global$skill$execute_script`,指定 skill_name=技能名、script_path=脚本相对路径、args=[脚本参数信息]
                
                【路径使用严格规范】
                ⚠️ 重要：SKILL.md 中引用的所有路径都是**相对路径标识符**,必须原样使用：
                - 不要尝试修改、优化或"修正"路径格式（例如：不要去掉前缀，不要转换为绝对路径）
                - 不要解析路径的实际含义，它们只是资源的唯一标识符
                - 从 SKILL.md 或其他文档中看到的路径，必须**逐字复制**到工具参数中
                - 即使路径看起来"不正确"或"奇怪",也要原样保留
                
                ❌ 错误示例：
                - 修改路径的前缀部分
                - 将相对路径改为绝对路径
                - 对路径进行 URL 编码或解码
                
                ✅ 正确示例：
                - 文档中怎么写，参数就怎么填，保持完全一致
                
                不要使用其他工具 (如 search_tools、文件读写工具等) 来访问本技能的资源！
                
                【使用方法】
                用户意图描述，说明想要完成的任务。Skill 会根据你的意图提供具体的执行步骤和指导。
                
                【返回结果】
                Skill 正文内容 (SKILL.md 的 instructions 部分) 和下一步行动引导。
                
                【典型用途】
                - 启动一个专门的技能来处理特定任务
                - 获取某个领域的专业知识和操作指南
                - 按照标准化的流程完成复杂工作
                
                【示例】
                用户："我需要写一份周报"
                → 调用 skill$weekly-report-writer
                ← 获得周报写作技能的完整 instructions
                ← 使用 global$skill$get_reference 读取模板文件
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
                        skill.body(),
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
