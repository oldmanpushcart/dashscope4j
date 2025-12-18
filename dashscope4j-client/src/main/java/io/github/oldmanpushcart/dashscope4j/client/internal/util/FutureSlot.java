package io.github.oldmanpushcart.dashscope4j.client.internal.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 独占式 Future 槽位管理器（基于能力令牌模型）。
 *
 * <p>本类为每个键（key）提供一个独占的 {@link CompletableFuture} 槽位，
 * 确保同一 key 在任意时刻最多只有一个活跃的异步操作。
 * 调用 {@link #acquire(Object)} 返回的 {@code CompletableFuture} 不仅表示操作结果，
 * 更是后续释放槽位的<strong>唯一能力令牌（capability token）</strong>。
 *
 * <p><strong>关键行为说明：</strong>
 * <ul>
 *   <li>{@link #complete(Object)} 和 {@link #completeExceptionally(Object, Throwable)}
 *       仅完成对应的 {@code Future}，<strong>不会释放槽位</strong>；
 *   <li>必须通过 {@link #release(Object, CompletableFuture)} 并传入<strong>原始 Future 引用</strong>
 *       才能真正释放该 key 的槽位；
 *   <li>这种设计确保“谁申请，谁释放”，防止误释放、并发冲突或状态混乱。
 * </ul>
 *
 * <p>典型应用场景：
 * <ul>
 *   <li>防止对同一连接 ID（如 WebSocket、数据库连接）重复建立连接；
 *   <li>确保任务 ID 或请求 ID 在系统中不被并发处理；
 *   <li>实现“请求去重 + 异步结果通知 + 显式资源回收”的安全机制。
 * </ul>
 *
 * <p>所有方法均为线程安全。
 *
 * @param <K> 槽位键的类型（如连接 ID、任务 ID 等）
 */
public class FutureSlot<K> {

    private final ConcurrentHashMap<K, CompletableFuture<Void>> futureMap = new ConcurrentHashMap<>();

    /**
     * 为指定键申请一个独占的 Future 槽位。
     *
     * <p>成功时返回一个未完成的 {@link CompletableFuture} 实例，
     * 该实例即为释放槽位的<strong>唯一凭证</strong>。
     * 若该键已被占用（即已有未释放的槽位），则抛出异常。
     *
     * @param key 槽位键，不可为 {@code null}
     * @return 新创建的、未完成的 {@link CompletableFuture}（作为能力令牌）
     * @throws IllegalStateException 如果该键已被占用
     */
    public synchronized CompletableFuture<Void> acquire(K key) {
        final var future = new CompletableFuture<Void>();
        if (null != futureMap.putIfAbsent(key, future)) {
            throw new IllegalStateException("%s already acquired!".formatted(key));
        }
        return future;
    }

    /**
     * 尝试以成功状态完成与指定键关联的 Future。
     *
     * <p><strong>注意：</strong>此操作仅将 Future 标记为完成，
     * <strong>不会从内部映射中移除该键</strong>。
     * 槽位仍处于占用状态，必须显式调用 {@link #release(Object, CompletableFuture)}
     * 并传入原始 Future 引用才能释放槽位，使该键可被再次使用。
     *
     * @param key 槽位键
     * @return {@code true} 表示 Future 存在且被成功完成；{@code false} 表示槽位不存在或已完成
     */
    public boolean complete(K key) {
        final var future = futureMap.get(key);
        return null != future && future.complete(null);
    }

    /**
     * 尝试以异常状态完成与指定键关联的 Future。
     *
     * <p><strong>注意：</strong>此操作仅设置 Future 的异常完成状态，
     * <strong>不会释放槽位</strong>。
     * 必须通过 {@link #release(Object, CompletableFuture)} 显式释放，
     * 且需提供原始 Future 引用。
     *
     * @param key 槽位键
     * @param ex 导致失败的异常，不可为 {@code null}
     * @return {@code true} 表示 Future 存在且被成功异常完成；{@code false} 表示槽位不存在或已完成
     */
    public boolean completeExceptionally(K key, Throwable ex) {
        final var future = futureMap.get(key);
        return null != future && future.completeExceptionally(ex);
    }

    /**
     * 显式释放指定键的槽位。
     *
     * <p>仅当内部存储的 Future 引用与传入的 {@code future} 完全相同时，
     * 才会移除该键。这确保了<strong>只有持有原始 Future 引用的调用方才能释放槽位</strong>，
     * 有效防止误操作或恶意释放。
     *
     * <p>正常流程中，应在完成 Future 后（通过 {@code complete} 或 {@code completeExceptionally}）
     * 调用本方法进行最终清理。
     *
     * @param key 槽位键
     * @param future 原始获取的 {@link CompletableFuture} 引用（作为释放凭证）
     * @return {@code true} 表示成功释放；{@code false} 表示键不存在或 Future 引用不匹配
     */
    public boolean release(K key, CompletableFuture<Void> future) {
        return futureMap.remove(key, future);
    }


    public CompletableFuture<Void> get(K key) {
        return futureMap.get(key);
    }

    /**
     * 原子地导出并清空所有当前活跃的槽位。
     *
     * <p>此方法会：
     * <ul>
     *   <li>获取当前所有键与对应 {@link CompletableFuture} 的完整快照；
     *   <li>清空内部状态，使所有键可被重新 {@link #acquire(Object)}；
     *   <li>返回该快照的副本（调用方拥有完全控制权）。
     * </ul>
     *
     * <p><strong>线程安全保证：</strong>
     * 本方法使用 {@code synchronized} 关键字确保快照与清空操作的原子性。
     * 在执行期间，所有其他方法（如 {@link #acquire(Object)}、{@link #release(Object, CompletableFuture)} 等）
     * 将被阻塞，从而避免在快照生成后、清空前有新槽位插入而导致丢失。
     *
     * <p><strong>注意：</strong>本方法<strong>不会自动完成或取消</strong>任何 Future，
     * 仅将其从管理器中移除。调用方需自行决定如何处理返回的 Futures（例如批量完成、取消或记录日志）。
     *
     * <p>典型用途包括系统优雅关闭、批量清理悬挂任务或调试时获取当前活跃操作快照。
     *
     * @return 包含所有被导出槽位的不可变映射；若无活跃槽位，则返回空映射
     */
    public synchronized Map<K, CompletableFuture<Void>> drain() {
        final var drained = new ConcurrentHashMap<>(futureMap);
        futureMap.clear();
        return drained;
    }

}
