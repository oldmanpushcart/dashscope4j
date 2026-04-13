package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill.provider;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill.Skill;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * Skill 提供者接口
 * <p>
 * 负责从不同来源（文件系统、ZIP、HTTP 等）提供一个或多个 Skill。
 * </p>
 * 
 * @since 4.0.0
 */
public interface SkillProvider {
    
    /**
     * 提供 Skills
     * <p>
     * 每次调用都会重新加载，支持外部触发刷新。
     * 可以返回 0 个或多个 Skill。
     * </p>
     * 
     * @return Skill 列表的异步回调（可能为空）
     */
    CompletionStage<List<Skill>> provide();

}
