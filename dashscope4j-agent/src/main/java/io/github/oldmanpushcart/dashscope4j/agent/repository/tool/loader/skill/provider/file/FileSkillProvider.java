package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill.provider.file;

import io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill.Skill;
import io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill.provider.SkillProvider;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

/**
 * 基于文件系统的 Skill 提供者
 *
 * <p>职责划分：</p>
 * <ul>
 *     <li>FileMonitor: 监听文件变化，加载 skill 并更新草稿版本</li>
 *     <li>FileSyncer: 专心负责草稿版本和发布版本的同步</li>
 *     <li>FileSkillProvider: 持有共享数据（草稿集合和正式集合），协调 Monitor 和 Syncer 工作</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class FileSkillProvider implements SkillProvider {

    private static final Logger logger = LoggerFactory.getLogger(FileSkillProvider.class);

    // === Configuration ===
    private final Path baseDir;
    private final boolean blocking;
    private final String _toString;

    // === Lifecycle ===
    private final CompletableFuture<Void> closeF = new CompletableFuture<>();
    private final CompletableFuture<Void> initF = new CompletableFuture<>();
    private final Monitor monitor;
    private final Syncer syncer;

    // === Shared Data (Managed by Monitor) ===
    // 草稿集合由 Monitor 持有，Provider 通过 monitor.getDrafts() 访问
    // 正式集合：skillName -> Entry (由 Syncer 更新)
    private final Map<String, Entry> actives = Collections.synchronizedMap(new HashMap<>());

    // === Runtime State ===
    private volatile Updater updater;

    /**
     * 私有构造函数，通过 Builder 创建实例
     */
    private FileSkillProvider(Builder builder) {
        this.baseDir = builder.baseDir.toAbsolutePath().normalize();
        this.blocking = builder.blocking;
        this._toString = "dashscope4j-agent:/skill/provider/file/%s".formatted(this.baseDir.getFileName());

        // 创建 Syncer - 负责草稿和发布版本的同步
        this.syncer = new Syncer(createSyncHandler(), builder.syncInterval, this.baseDir.getFileName().toString());

        // 创建 Monitor - 负责监听文件变化并管理草稿集合
        this.monitor = new Monitor(this.baseDir, createMonitorChangeHandler());
    }

    @Override
    public String toString() {
        return _toString;
    }

    /**
     * 创建 Builder
     *
     * @return Builder 实例
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    @Override
    public CompletionStage<Void> init(Updater updater) {
        this.updater = requireNonNull(updater, "updater must not be null");

        if (closeF.isDone()) {
            throw new IllegalStateException("Already closed!");
        }

        if (!initF.complete(null)) {
            throw new IllegalStateException("Already initialized");
        }

        try {

            // 验证基础目录
            if (!Files.exists(baseDir)) {
                throw new IOException("Base directory does not exist: " + baseDir);
            }

            if (!Files.isDirectory(baseDir)) {
                throw new IOException("Path is not a directory: " + baseDir);
            }

            // 启动同步器
            this.syncer.start();

            // 启动监控器
            this.monitor.start();

            // 扫描并加载所有 Skills（初始化草稿版本）
            final var skills = scanAllSkills();

            if (blocking) {
                // Blocking 模式：必须所有 skill 都加载成功
                return loadAllSkillsBlocking(skills)
                        .thenAccept(v -> logger.info("Loaded {} skills (blocking mode)", skills.size()));
            } else {
                // Non-blocking 模式：单个 skill 失败不影响整体
                return loadAllSkillsNonBlocking(skills)
                        .thenAccept(v -> logger.info("Loaded {} skills (non-blocking mode)", skills.size()));
            }

        } catch (Exception e) {
            cleanupOnFailure();
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * 创建变更处理器 - Monitor 使用此处理器通知草稿集合变更（直接传递 Entry）
     */
    private Monitor.ChangeHandler createMonitorChangeHandler() {
        return (entry, delete) -> {
            String skillName = entry.skill().name();
            logger.debug("Draft changed: {}, delete={}", skillName, delete);
            // Monitor 已经管理了 drafts，这里只需要通知 Syncer
            if (delete) {
                syncer.notifyDelete(skillName);
            } else {
                syncer.notifyChange(entry);  // ✅ 直接传递 Entry 给 Syncer
            }
        };
    }

    /**
     * 创建同步处理器 - Syncer 使用此处理器执行实际同步
     */
    private Syncer.Handler createSyncHandler() {
        return (skillsToUpsert, skillsToRemove) -> {
            // Upsert 技能
            skillsToUpsert.forEach(skillName -> {
                try {
                    // 从 Monitor 的草稿集合获取 Entry
                    Entry entry = monitor.getDrafts().get(skillName);
                    if (entry != null) {
                        updater.upsert(entry.skill()).toCompletableFuture().join();
                        synchronized (actives) {
                            actives.put(skillName, entry);
                        }
                        logger.debug("Syncer upserted skill: {}", skillName);
                    }
                } catch (Exception e) {
                    logger.warn("Syncer failed to upsert skill: {}", skillName, e);
                }
            });

            // Remove 技能
            skillsToRemove.forEach(skillName -> {
                try {
                    synchronized (actives) {
                        actives.remove(skillName);
                    }
                    updater.remove(skillName).toCompletableFuture().join();
                    logger.debug("Syncer removed skill: {}", skillName);
                } catch (Exception e) {
                    logger.warn("Syncer failed to remove skill: {}", skillName, e);
                }
            });
        };
    }

    /**
     * 安全加载单个 Skill（失败时不抛异常）
     */
    private Skill loadSkillSafely(Path skillDir) {
        try {
            return new FileSkill(skillDir);
        } catch (Exception e) {
            logger.warn("Failed to load skill from: {}", skillDir, e);
            return null;
        }
    }

    /**
     * 扫描目录下所有 Skill（仅扫描，不加载）
     */
    private List<Skill> scanAllSkills() {
        try (var paths = Files.walk(baseDir)) {
            return paths
                    .filter(Files::isDirectory)
                    .filter(skillDir -> Files.exists(skillDir.resolve("SKILL.md")))
                    .map(this::loadSkillSafely)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.warn("Failed to scan skills in directory: {}", baseDir, e);
            return List.of();
        }
    }

    /**
     * 初始化失败时清理资源
     */
    private void cleanupOnFailure() {
        if (monitor != null) {
            monitor.close();
        }
        if (syncer != null) {
            syncer.close();
        }
    }

    /**
     * Blocking 模式：必须所有 skill 都加载成功
     */
    private CompletionStage<Void> loadAllSkillsBlocking(List<Skill> skills) {
        if (skills.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        final var stages = new ArrayList<CompletionStage<Void>>();
        for (Skill skill : skills) {
            stages.add(updater.upsert(skill));
        }

        return CompletableFutureUtils.allOf(10, stages)
                .thenRun(() -> {
                    synchronized (actives) {
                        skills.forEach(skill ->
                                findSkillHandle(skill.name())
                                        .ifPresent(path ->
                                                actives.put(skill.name(), new Entry(skill, path))));
                    }
                });
    }

    /**
     * Non-blocking 模式：单个 skill 失败不影响其他 skill
     */
    private CompletionStage<Void> loadAllSkillsNonBlocking(List<Skill> skills) {
        if (skills.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        final var loadedSkills = new ArrayList<Skill>();
        for (Skill skill : skills) {
            try {
                updater.upsert(skill).toCompletableFuture().join();
                loadedSkills.add(skill);
            } catch (Exception e) {
                logger.warn("Failed to load skill: {} (non-blocking mode continues)", skill.name(), e);
            }
        }

        // 更新正式集合
        synchronized (actives) {
            loadedSkills.forEach(skill ->
                    findSkillHandle(skill.name()).ifPresent(path ->
                            actives.put(skill.name(), new Entry(skill, path))));
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 查找 Skill 路径
     */
    private Optional<Path> findSkillHandle(String skillName) {
        try (Stream<Path> paths = Files.walk(baseDir)) {
            return paths
                    .filter(Files::isDirectory)
                    .filter(dir -> dir.getFileName().toString().equals(skillName))
                    .filter(dir -> Files.exists(dir.resolve("SKILL.md")))
                    .findFirst();
        } catch (IOException e) {
            logger.warn("Failed to find skill directory: {}", skillName, e);
            return Optional.empty();
        }
    }

    @Override
    public void close() {
        // Signal shutdown
        if (!closeF.complete(null)) {
            return;
        }

        logger.debug("{} closing...", this);

        // 关闭监控器
        if (monitor != null) {
            monitor.close();
        }

        // 关闭同步器
        if (syncer != null) {
            syncer.close();
        }

        // Remove all Skills
        if (updater != null) {
            List.copyOf(actives.keySet()).forEach(name -> {
                updater.remove(name);
            });
            updater = null;
        }

        // Clear shared data
        synchronized (actives) {
            actives.clear();
        }
        // Monitor 的 drafts 会在 Monitor 关闭时自动清理

        logger.debug("{} closed", this);
    }

    /**
     * Builder 类 - 用于构造 FileSkillProvider
     */
    public static class Builder {

        private Path baseDir;
        private boolean blocking = true;
        private Duration syncInterval = Duration.ofSeconds(30); // 默认 30 秒检查一次

        /**
         * 设置基础目录
         *
         * @param baseDir 基础目录路径
         * @return Builder 实例
         */
        public Builder baseDir(Path baseDir) {
            this.baseDir = Objects.requireNonNull(baseDir, "baseDir must not be null");
            return this;
        }

        /**
         * 设置基础目录
         *
         * @param path 基础目录路径字符串
         * @return Builder 实例
         */
        public Builder baseDir(String path) {
            return baseDir(Paths.get(path));
        }

        /**
         * 设置 blocking 加载策略
         * <p>
         * blocking=true: 单个 skill 加载失败不影响 init 结果<br>
         * blocking=false: 必须所有 skill 都加载完成 init 才完成
         * </p>
         *
         * @param blocking true=容错模式，false=严格模式
         * @return Builder 实例
         */
        public Builder blocking(boolean blocking) {
            this.blocking = blocking;
            return this;
        }

        /**
         * 设置同步检查间隔
         * <p>
         * 同步器会定期检查文件系统状态，并在检测到不一致时自动修复
         * </p>
         *
         * @param syncInterval 检查间隔
         * @return Builder 实例
         */
        public Builder syncInterval(Duration syncInterval) {
            this.syncInterval = requireNonNull(syncInterval, "syncInterval must not be null");
            return this;
        }

        /**
         * 构建 FileSkillProvider 实例
         *
         * @return FileSkillProvider 实例
         */
        public FileSkillProvider build() {
            Objects.requireNonNull(baseDir, "baseDir must be specified");
            return new FileSkillProvider(this);
        }
    }
}