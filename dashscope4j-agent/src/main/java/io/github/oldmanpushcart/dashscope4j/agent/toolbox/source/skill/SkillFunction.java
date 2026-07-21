package io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.skill;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.function.Function;

/**
 * 技能函数
 */
class SkillFunction implements Function<SkillFunction.Spec, String> {

    private final Skill skill;

    public SkillFunction(Skill skill) {
        this.skill = skill;
    }

    @Override
    public String apply(Spec spec) {
        return """
                %s
                
                ---
                
                ## 技能信息
                - 技能名称：%s
                - 基础路径：%s
                
                如果技能描述中引用的路径是相对路径，请拼接`基础路径`进行访问。
                已激活技能，现在请开始执行任务...
                """.formatted(
                skill.body(),
                skill.header().name(),
                skill.home()
        );
    }

    public Tool asTool() {
        return FunctionTool.newBuilder()
                .name("skill$%s".formatted(skill.header().name()))
                .description(skill.header().description())
                .parameterType(Spec.class)
                .function(this)
                .build();
    }

    /**
     * 用户意图参数 (用于 skill$<skill_name> 工具)
     */
    record Spec(
            @JsonProperty("intent")
            @JsonPropertyDescription("用户意图描述，说明想要完成的任务")
            String intent
    ) {
    }

}
