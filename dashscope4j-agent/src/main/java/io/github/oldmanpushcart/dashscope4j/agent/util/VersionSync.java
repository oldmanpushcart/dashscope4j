package io.github.oldmanpushcart.dashscope4j.agent.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 版本同步控制器
 * <p>
 * 使用一个 {@code AtomicLong} 存储两个{@code int}类型的版本号：
 * - 高 32 位：active version（已激活的版本）
 * - 低 32 位：staged version（暂存的版本）
 * </p>
 * <p>
 * 这种设计可以保证两个版本号的原子性更新，同时节省内存空间。
 * 适用于需要对比和更新两个相关版本号的并发场景。
 * </p>
 */
public final class VersionSync {

    /**
     * 版本号对，高 32 位为 active，低 32 位为 staged
     */
    private final AtomicLong versionPair;

    /**
     * 构造函数
     */
    public VersionSync() {
        this(0);
    }

    /**
     * 构造函数
     *
     * @param initialVersion 初始版本号
     */
    public VersionSync(int initialVersion) {
        this.versionPair = new AtomicLong(initialVersion & 0xFFFFFFFFL);
    }

    /**
     * 增加 staged 版本
     * <p>
     * staged version 自增 1，active version 保持不变。
     * 当有新工具列表时调用此方法标记有变更需要同步。
     * </p>
     */
    public void incrementStaged() {
        while (true) {
            final long current = versionPair.get();
            final long staged = (current & 0xFFFFFFFFL) + 1;
            final long next = (current & 0xFFFFFFFF00000000L) | staged;
            if (versionPair.compareAndSet(current, next)) {
                return;
            }
        }
    }

    /**
     * @return active 版本（已激活的版本）
     */
    public int active() {
        return (int) ((versionPair.get() >>> 32) & 0xFFFFFFFFL);
    }

    /**
     * @return staged 版本（暂存的版本）
     */
    public int staged() {
        return (int) (versionPair.get() & 0xFFFFFFFFL);
    }

    /**
     * 设置 active 版本
     * <p>
     * 将 staged version 的值同步到 active version，表示同步完成。
     * </p>
     *
     * @param version 新的版本号
     */
    public void activate(int version) {
        versionPair.set((versionPair.get() & 0xFFFFFFFFL) | ((long) version << 32));
    }

    /**
     * 检查是否有未同步的变更
     * <p>
     * 当 active version 与 staged version 不相等时，表示有待同步的变更。
     * </p>
     *
     * @return true 如果需要同步，否则返回 false
     */
    public boolean hasChanges() {
        return active() != staged();
    }

    /**
     * 重置版本号
     * <p>
     * 将 active version 和 staged version 都重置为 0。
     * </p>
     */
    public void reset() {
        versionPair.set(0);
    }

    /**
     * 获取当前版本对的字符串表示
     *
     * @return 格式为 "active/staged"
     */
    @Override
    public String toString() {
        return "%d/%d".formatted(active(), staged());
    }

}
