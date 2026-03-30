package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill.provider.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 技能同步器 - 基于版本控制的后台同步线程
 * <p>
 * 维护两个版本集合：
 * - 草稿版 (draft): 反映当前文件系统的实际状态（实时变更）
 * - 发布版 (active): 已成功同步到 updater 的状态
 * </p>
 * <p>
 * 工作机制：
 * - 定期唤醒：每隔固定周期检查版本差异并同步
 * - 主动唤醒：当检测到文件变更时立即唤醒进行检查
 * - 失败容忍：同步失败则放弃本轮，等待下个周期
 * </p>
 */
class FileSyncer extends Thread implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(FileSyncer.class);

    // === Configuration ===
    private final Path baseDir;
    private final Duration syncInterval;
    private final SyncHandler handler;
    private final String _toString;

    // === Version Management ===
    // skillName -> version (单调递增)
    private final Map<String, AtomicLong> draftVersions = new HashMap<>();
    private final Map<String, Long> activeVersions = new HashMap<>();

    // === Synchronization ===
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition wakeupCondition = lock.newCondition();

    // === Lifecycle ===
    private volatile boolean closed = false;

    /**
     * 同步处理器接口
     */
    interface SyncHandler {
        /**
         * 执行同步操作
         *
         * @param skillsToUpsert 需要新增或更新的技能列表
         * @param skillsToRemove 需要删除的技能列表
         */
        void sync(List<String> skillsToUpsert, List<String> skillsToRemove);
    }

    /**
     * 创建同步器
     *
     * @param baseDir      基础目录
     * @param handler      同步处理器
     * @param syncInterval 同步检查间隔
     */
    FileSyncer(Path baseDir, SyncHandler handler, Duration syncInterval) {
        super("FileSyncer-%s".formatted(baseDir.toAbsolutePath().getFileName()));
        this.baseDir = baseDir.toAbsolutePath().normalize();
        this.handler = handler;
        this.syncInterval = syncInterval;
        this._toString = "dashscope4j-agent:/skill/syncer/%s".formatted(this.baseDir.getFileName());
        this.setDaemon(true);
    }

    @Override
    public String toString() {
        return _toString;
    }

    /**
     * 启动同步线程
     */
    void startSyncing() {
        super.start();
        logger.info("{} started with interval={}", this, syncInterval);
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
     * 通知同步器：指定技能发生变更
     *
     * @param skillName 技能名称
     */
    void notifyChange(String skillName) {
        lock.lock();
        try {
            // 草稿版 version + 1
            draftVersions.computeIfAbsent(skillName, k -> new AtomicLong(0))
                    .incrementAndGet();

            logger.debug("{} skill changed: {} (draft version updated)", this, skillName);

            // 主动唤醒 syncer
            wakeupCondition.signal();

        } finally {
            lock.unlock();
        }
    }

    /**
     * 通知同步器：指定技能被删除
     *
     * @param skillName 技能名称
     */
    void notifyDelete(String skillName) {
        lock.lock();
        try {
            // 从草稿版移除
            draftVersions.remove(skillName);

            logger.debug("{} skill deleted: {}", this, skillName);

            // 主动唤醒 syncer
            wakeupCondition.signal();

        } finally {
            lock.unlock();
        }
    }

    /**
     * 执行同步：比较草稿版和发布版，同步差异
     */
    private void performSync() {
        try {
            // 刷新草稿版：确保所有存在的技能都在草稿版中
            refreshDraftVersions();

            // 计算差异
            final var skillsToUpsert = new ArrayList<String>();
            final var skillsToRemove = new ArrayList<String>();

            // 查找需要 upsert 的技能：
            // 1. 草稿版有但发布版没有（新增）
            // 2. 草稿版和发布版都有，但 version 不同（更新）
            draftVersions.forEach((skillName, draftVersion) -> {
                Long activeVersion = activeVersions.get(skillName);
                if (activeVersion == null || activeVersion != draftVersion.get()) {
                    skillsToUpsert.add(skillName);
                }
            });

            // 查找需要 remove 的技能：
            // 发布版有但草稿版没有（删除）
            activeVersions.keySet().stream()
                    .filter(name -> !draftVersions.containsKey(name))
                    .forEach(skillsToRemove::add);

            // 如果没有差异，直接返回
            if (skillsToUpsert.isEmpty() && skillsToRemove.isEmpty()) {
                logger.trace("{} no changes to sync", this);
                return;
            }

            // 执行同步
            logger.info("{} syncing: upsert={}, remove={}", this, skillsToUpsert, skillsToRemove);
            handler.sync(skillsToUpsert, skillsToRemove);

            // 同步成功后，更新发布版
            draftVersions.forEach((name, version) -> 
                activeVersions.put(name, version.get()));
            skillsToRemove.forEach(activeVersions::remove);

            logger.info("{} synced successfully", this);

        } catch (Exception e) {
            logger.warn("{} sync failed, will retry in next cycle", this, e);
            // ← 关键点：捕获异常但不抛，等待下个周期重试
        }
    }

    /**
     * 刷新草稿版：扫描文件系统，确保所有存在的技能都在草稿版中
     */
    private void refreshDraftVersions() {
        try (Stream<Path> paths = Files.walk(baseDir, 3)) {
            final var existingSkills = paths
                    .filter(Files::isDirectory)
                    .filter(dir -> Files.exists(dir.resolve("SKILL.md")))
                    .map(dir -> dir.getFileName().toString())
                    .collect(Collectors.toSet());

            // 保留现有技能，移除不存在的
            draftVersions.keySet().retainAll(existingSkills);

            // 添加新发现的技能（version 初始为 0）
            existingSkills.forEach(name -> 
                draftVersions.computeIfAbsent(name, k -> new AtomicLong(0)));

        } catch (Exception e) {
            logger.warn("{} failed to refresh draft versions", this, e);
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
