package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill.provider.file;

import io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 技能同步器 - 专心负责草稿版本和发布版本的同步
 * <p>
 * 职责：
 * 1. 维护两个版本集合：
 *    - 草稿版 (draft): Monitor 直接更新共享的 drafts 集合（通过版本号感知变化）
 *    - 发布版 (active): 已成功同步到 updater 的状态
 * 2. 定期比较两个版本集合，执行差异同步
 * 3. 支持主动唤醒进行同步
 * </p>
 * <p>
 * 注意：本类不持有 Skill 对象，只处理名称和版本号！
 * </p>
 */
class Syncer extends Thread implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(Syncer.class);

    // === Configuration ===
    private final Duration syncInterval;
    private final Handler handler;
    private final String _toString;

    // === Version Management ===
    // skillName -> version (单调递增，由 Monitor 触发)
    private final Map<String, AtomicLong> draftVersions = new HashMap<>();
    private final Map<String, Long> activeVersions = new HashMap<>();

    // === Shared Data (Managed by Syncer) ===
    // 草稿版本：skillName -> Entry (从 Monitor 传递过来)
    private final Map<String, Entry> draftEntries = Collections.synchronizedMap(new HashMap<>());
    // 发布版本：skillName -> Entry (已成功同步的)

    // === Synchronization ===
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition wakeupCondition = lock.newCondition();

    // === Lifecycle ===
    private volatile boolean closed = false;

    /**
     * 同步处理器接口
     */
    interface Handler {
        
        /**
         * 执行同步操作
         *
         * @param toUpserts 需要新增或更新的技能列表
         * @param toRemoves 需要删除的技能列表
         */
        void sync(List<String> toUpserts, List<String> toRemoves);
        
    }

    /**
     * 创建同步器
     *
     * @param handler      同步处理器
     * @param syncInterval 同步检查间隔
     * @param baseDirName  基础目录名称（用于标识）
     */
    Syncer(Handler handler, Duration syncInterval, String baseDirName) {
        super("FileSyncer-" + baseDirName);
        this.handler = handler;
        this.syncInterval = syncInterval;
        this._toString = "dashscope4j-agent:/skill/syncer/%s".formatted(baseDirName);
        this.setDaemon(true);
    }

    @Override
    public String toString() {
        return _toString;
    }

    @Override
    public void run() {
        logger.debug("{} thread started", this);

        try {
            while (!closed && !Thread.currentThread().isInterrupted()) {
                // 等待定时唤醒或提前唤醒
                lock.lock();
                try {
                    logger.trace("{} waiting for {} or signal", this, syncInterval);
                    wakeupCondition.await(syncInterval.toMillis(), TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } finally {
                    lock.unlock();
                }

                // 执行同步
                if (!closed) {
                    performSync();
                }
            }
        } finally {
            logger.debug("{} thread stopped", this);
        }
    }

    /**
     * 通知同步器：指定技能发生变更（直接传递 Entry）
     *
     * @param entry 技能条目（包含 Skill 和 Path）
     */
    void notifyChange(Entry entry) {
        lock.lock();
        try {
            String skillName = entry.skill().name();
            
            // 更新草稿版本
            draftEntries.put(skillName, entry);
            
            // 草稿版 version + 1
            draftVersions
                    .computeIfAbsent(skillName, k -> new AtomicLong(0))
                    .incrementAndGet();

            logger.debug("{} skill changed: {} (draft version updated)", this, skillName);

            // 主动唤醒 syncer
            wakeupCondition.signal();

        } finally {
            lock.unlock();
        }
    }

    /**
     * 通知同步器：指定技能被删除（从草稿版本移除）
     *
     * @param skillName 技能名称
     */
    void notifyDelete(String skillName) {
        lock.lock();
        try {
            // 从草稿版移除
            draftEntries.remove(skillName);
            draftVersions.remove(skillName);

            logger.debug("{} skill deleted: {}", this, skillName);

            // 主动唤醒 syncer
            wakeupCondition.signal();

        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取草稿版本的 Skill（包级私有访问权限，已废弃）
     *
     * @param skillName 技能名称
     * @return Skill 对象，如果不存在则返回 null
     * @deprecated 直接使用 Provider 的 drafts 集合
     */
    @Deprecated
    Skill getDraftSkill(String skillName) {
        return null; // 不再持有 Skill 引用
    }

    /**
     * 执行同步：比较草稿版和发布版，同步差异
     */
    private void performSync() {
        try {
            // 计算差异
            final var toUpserts = new ArrayList<Entry>();
            final var toRemoves = new ArrayList<String>();

            // 查找需要 upsert 的技能：
            // 1. 草稿版有但发布版没有（新增）
            // 2. 草稿版和发布版都有，但 version 不同（更新）
            draftVersions.forEach((skillName, draftVersion) -> {
                Long activeVersion = activeVersions.get(skillName);
                if (activeVersion == null || activeVersion != draftVersion.get()) {
                    Entry entry = draftEntries.get(skillName);
                    if (entry != null) {
                        toUpserts.add(entry);
                    }
                }
            });

            // 查找需要 remove 的技能：
            // 发布版有但草稿版没有（删除）
            activeVersions.keySet().stream()
                    .filter(name -> !draftVersions.containsKey(name))
                    .forEach(toRemoves::add);

            // 如果没有差异，直接返回
            if (toUpserts.isEmpty() && toRemoves.isEmpty()) {
                logger.trace("{} no changes to sync", this);
                return;
            }

            // 执行同步
            logger.info("{} syncing: upsert={}, remove={}", this, 
                toUpserts.stream().map(e -> e.skill().name()).toList(), toRemoves);
            handler.sync(
                toUpserts.stream().map(e -> e.skill().name()).toList(),
                toRemoves
            );

            // 同步成功后，更新发布版
            toUpserts.forEach(entry -> {
                String skillName = entry.skill().name();
                activeVersions.put(skillName, draftVersions.get(skillName).get());
            });
            toRemoves.forEach(activeVersions::remove);

            logger.info("{} synced successfully", this);

        } catch (Exception e) {
            logger.warn("{} sync failed, will retry in next cycle", this, e);
            // ← 关键点：捕获异常但不抛，等待下个周期重试
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        logger.debug("{} closing...", this);

        // 唤醒线程以退出等待
        lock.lock();
        try {
            wakeupCondition.signal();
        } finally {
            lock.unlock();
        }

        // 中断线程
        this.interrupt();

        logger.debug("{} closed", this);
    }

}
