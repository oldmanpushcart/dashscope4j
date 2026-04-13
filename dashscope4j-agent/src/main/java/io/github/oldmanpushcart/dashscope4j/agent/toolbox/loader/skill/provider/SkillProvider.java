package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill.provider;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.skill.Skill;

import java.util.List;
import java.util.concurrent.CompletableFuture;
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

    /**
     * 获取当前状态的签名标识
     * <p>
     * 用于快速判断 Skills 是否发生变化，无需完整加载。
     * 当签名值与上次不同时，表示内容已变更，SkillToolLoader 会重新调用 {@link #provide()} 加载。
     * </p>
     * <p>
     * 实现建议：
     * <ul>
     *   <li>文件型：基于关键文件（如 SKILL.md）的最后修改时间或内容哈希</li>
     *   <li>数据库型：基于最大更新时间或记录数的 checksum</li>
     *   <li>HTTP 型：优先使用 ETag 头，降级为响应内容哈希</li>
     *   <li>静态型：返回固定值或 "static"</li>
     * </ul>
     * </p>
     * <p>
     * 默认实现返回 "unknown"，表示不支持签名检测，调用方将跳过优化直接加载。
     * </p>
     * 
     * @return 签名字符串，如果无法确定则返回 "unknown"
     */
    CompletionStage<String> signature();

}
