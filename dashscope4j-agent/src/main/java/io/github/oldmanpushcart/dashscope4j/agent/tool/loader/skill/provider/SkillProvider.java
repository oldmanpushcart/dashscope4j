package io.github.oldmanpushcart.dashscope4j.agent.tool.loader.skill.provider;

import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.skill.Skill;

import java.util.concurrent.CompletionStage;

/**
 * Skill 提供者接口 - 负责从不同来源加载和管理 Skill
 * 
 * <p>实现类必须在 init() 失败时自动调用 close() 清理资源。</p>
 * 
 * @since 4.0.0
 */
public interface SkillProvider extends AutoCloseable {
    
    /**
     * 初始化 Provider
     * 
     * <p>如果初始化失败，实现类必须:</p>
     * <ol>
     *   <li>自动调用 close() 清理已分配的资源</li>
     *   <li>通过 updater 移除已注册的 Skill (如果有)</li>
     *   <li>抛出或传播异常</li>
     * </ol>
     * 
     * @param updater Skill 更新器，用于动态注册/更新/移除 Skill
     * @return 初始化完成的异步回调
     */
    CompletionStage<Void> init(Updater updater);
    
    /**
     * Skill 更新器接口
     */
    interface Updater {

        /**
         * 注册或更新 Skill
         * @param skill Skill 实例
         * @return 操作完成的异步回调
         */
        CompletionStage<Void> upsert(Skill skill);
        
        /**
         * 移除 Skill
         * @param name Skill 名称
         * @return 操作完成的异步回调
         */
        CompletionStage<Void> remove(String name);

    }
    
    @Override
    void close();

}
