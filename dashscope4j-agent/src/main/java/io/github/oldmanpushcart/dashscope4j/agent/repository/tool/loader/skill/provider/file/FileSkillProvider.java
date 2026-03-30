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
 * <p>功能特性：</p>
 * <ul>
 *     <li>单个 watcher 线程监听 baseDir 及其子目录变更</li>
 *     <li>支持 Builder 构造模式</li>
 *     <li>支持 blocking/non-blocking 两种加载策略</li>
 *     <li>每个子目录代表一个 Skill，目录名为 Skill 名称</li>
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
    private final FileMonitor monitor;
    private final FileSyncer syncer;

    // === Synchronization ===
    private final Map<String, Entry> skillsMap = new HashMap<>();

    // === Runtime State ===
    private volatile Updater updater;

    /**
     * 私有构造函数，通过 Builder 创建实例
     */
    private FileSkillProvider(Builder builder) {
        this.baseDir = builder.baseDir.toAbsolutePath().normalize();
        this.blocking = builder.blocking;
        this._toString = "dashscope4j-agent:/skill/provider/file/%s".formatted(this.baseDir.getFileName());
        this.monitor = new FileMonitor(this.baseDir, new FileMonitor.EventHandler() {

            @Override
            public void onFileCreate(Path path) {
                handleFileCreate(path);
            }

            @Override
            public void onFileDelete(Path path) {
                handleFileDelete(path);
            }

            @Override
            public void onFileModify(Path path) {
                handleFileModify(path);
            }

        });
        this.syncer = new FileSyncer(this.baseDir, createSyncHandler(), builder.syncInterval);
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

            // 启动监控线程
            this.monitor.start();

            // 启动同步器
            this.syncer.startSyncing();

            // 扫描并加载所有 Skills
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
     * 创建同步处理器
     */
    private FileSyncer.SyncHandler createSyncHandler() {
        return (skillsToUpsert, skillsToRemove) -> {
            // Upsert 技能
            skillsToUpsert.forEach(skillName -> {
                try {
                    var skillHandle = findSkillHandle(skillName);
                    if (skillHandle.isPresent()) {
                        var skill = loadSkillSafely(skillHandle.get());
                        if (skill != null) {
                            skillsMap.put(skillName, new Entry(skill, skillHandle.get()));
                            updater.upsert(skill).toCompletableFuture().join();
                            logger.info("Syncer upserted skill: {}", skillName);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Syncer failed to upsert skill: {}", skillName, e);
                }
            });

            // Remove 技能
            skillsToRemove.forEach(skillName -> {
                try {
                    if (skillsMap.containsKey(skillName)) {
                        skillsMap.remove(skillName);
                        updater.remove(skillName).toCompletableFuture().join();
                        logger.info("Syncer removed skill: {}", skillName);
                    }
                } catch (Exception e) {
                    logger.warn("Syncer failed to remove skill: {}", skillName, e);
                }
            });
        };
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
     * 处理文件创建事件
     */
    private void handleFileCreate(Path fullPath) {

        // 检查是否为新目录
        if (Files.isDirectory(fullPath) && Files.exists(fullPath.resolve("SKILL.md"))) {
            String skillName = fullPath.getFileName().toString();

            synchronized (skillsMap) {
                if (!skillsMap.containsKey(skillName)) {
                    Skill skill = loadSkillSafely(fullPath);
                    if (skill != null) {
                        skillsMap.put(skillName, new Entry(skill, baseDir.relativize(fullPath)));
                        updater.upsert(skill).toCompletableFuture().join();
                        logger.info("Loaded new skill: {}", skillName);
                    }
                }
            }

        }

        // 通知同步器版本变更
        String skillName = fullPath.getFileName().toString();
        syncer.notifyChange(skillName);
    }

    /**
     * 处理文件删除事件
     */
    private void handleFileDelete(Path fullPath) {
        String skillName = fullPath.getFileName().toString();

        synchronized (skillsMap) {
            if (skillsMap.containsKey(skillName)) {
                skillsMap.remove(skillName);
                updater.remove(skillName).toCompletableFuture().join();
                logger.info("Removed skill: {}", skillName);
            }
        }

        // 通知同步器删除
        syncer.notifyDelete(skillName);
    }

    /**
     * 处理文件修改事件
     */
    private void handleFileModify(Path fullPath) {
        // 检查是否为 SKILL.md 文件修改
        if (fullPath.getFileName().toString().equals("SKILL.md") && Files.isDirectory(fullPath.getParent())) {
            Path skillDir = fullPath.getParent();
            String skillName = skillDir.getFileName().toString();

            synchronized (skillsMap) {
                Skill skill = loadSkillSafely(skillDir);
                if (skill != null) {
                    updater.upsert(skill).toCompletableFuture().join();
                    logger.info("Updated skill: {}", skillName);
                }
            }
        }

        // 通知同步器版本变更
        if (Files.isDirectory(fullPath.getParent())) {
            String skillName = fullPath.getParent().getFileName().toString();
            syncer.notifyChange(skillName);
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
                    synchronized (skillsMap) {
                        skills.forEach(skill ->
                                findSkillHandle(skill.name())
                                        .ifPresent(path -> 
                                                skillsMap.put(skill.name(), new Entry(skill, path))));
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

        // 更新 skillsMap
        synchronized (skillsMap) {
            loadedSkills.forEach(skill ->
                findSkillHandle(skill.name()).ifPresent(path ->
                        skillsMap.put(skill.name(), new Entry(skill, path))));
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
            List.copyOf(skillsMap.keySet()).forEach(name -> {
                updater.remove(name);
            });
            updater = null;
        }

        // Clear storage
        synchronized (skillsMap) {
            skillsMap.clear();
        }

        logger.debug("{} closed", this);
    }

    /**
     * Skill 条目 - 内部记录类
     */
    private record Entry(Skill skill, Path path) {
        String name() {
            return skill.name();
        }
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