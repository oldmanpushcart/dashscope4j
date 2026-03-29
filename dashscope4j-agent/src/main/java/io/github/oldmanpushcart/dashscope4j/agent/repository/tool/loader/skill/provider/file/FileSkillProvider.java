package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill.provider.file;

import io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill.Skill;
import io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill.provider.SkillProvider;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.*;
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

    // === Synchronization ===
    private final Map<String, Path> skillsMap = new HashMap<>();

    // === Runtime State ===
    private final Thread watcher;
    private Updater updater;
    private WatchService watchService;

    /**
     * 私有构造函数，通过 Builder 创建实例
     */
    private FileSkillProvider(Builder builder) {
        this.baseDir = builder.baseDir.toAbsolutePath().normalize();
        this.blocking = builder.blocking;
        this._toString = "dashscope4j-agent:/skill/provider/file/%s".formatted(this.baseDir.getFileName());
        this.watcher = new Thread(this::watching, "%s/watcher".formatted(this));
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

            // 创建 WatchService
            this.watchService = baseDir.getFileSystem().newWatchService();

            // 递归注册所有现有子目录
            registerDirectory(baseDir);

            // 扫描并加载所有 Skills
            List<Skill> skills = scanAllSkills();

            // 启动 watcher 线程
            startWatcher();

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
     * 递归注册目录监听
     */
    private void registerDirectory(Path dir) throws IOException {
        dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        logger.debug("Registered directory for watching: {}", dir);

        // 递归注册所有子目录
        try (Stream<Path> paths = Files.list(dir)) {
            paths.filter(Files::isDirectory)
                    .forEach(child -> {
                        try {
                            registerDirectory(child);
                        } catch (IOException e) {
                            logger.warn("Failed to register child directory: {}", child, e);
                        }
                    });
        }
    }

    /**
     * 扫描目录下所有 Skill（仅扫描，不加载）
     */
    private List<Skill> scanAllSkills() {
        try (Stream<Path> paths = Files.walk(baseDir)) {
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
     * Blocking 模式：必须所有 skill 都加载成功才完成 init
     */
    private CompletionStage<Void> loadAllSkillsBlocking(List<Skill> skills) {
        if (skills.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        final var stages = new ArrayList<CompletionStage<Void>>();
        for (Skill skill : skills) {
            stages.add(updater.upsert(skill));
        }

        // 等待所有 skill 加载完成，任何一个失败都会导致整体失败
        return CompletableFutureUtils.allOf(10, stages)
                .thenRun(() -> {
                    synchronized (skillsMap) {
                        skills.forEach(skill -> {
                            findSkillDir(skill.name())
                                    .ifPresent(dir ->
                                            skillsMap.put(skill.name(), baseDir.relativize(dir)));
                        });
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
            loadedSkills.forEach(skill -> {
                findSkillDir(skill.name()).ifPresent(dir ->
                        skillsMap.put(skill.name(), baseDir.relativize(dir)));
            });
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 查找 Skill 目录
     */
    private Optional<Path> findSkillDir(String skillName) {
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
     * 启动 watcher 线程
     */
    private void startWatcher() {
        this.watcher.setDaemon(true);
        this.watcher.start();
    }

    /**
     * Watcher 线程主循环
     */
    private void watching() {
        logger.debug("{}/watcher started", this);

        try {
            while (!closeF.isDone() && !Thread.currentThread().isInterrupted()) {
                // 等待事件或定期检查
                WatchKey key;
                try {
                    key = watchService.poll(1, TimeUnit.SECONDS);
                    if (key == null) {
                        continue;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                // 处理事件
                processEvents(key);
            }
        } finally {
            logger.debug("{}/watcher stopped", this);
        }

    }

    /**
     * 处理 Watch 事件
     */
    private void processEvents(WatchKey key) {
        for (WatchEvent<?> event : key.pollEvents()) {
            WatchEvent.Kind<?> kind = event.kind();
            Path context = (Path) event.context();

            if (context == null) continue;

            Path fullPath = ((Path) key.watchable()).resolve(context);

            try {
                if (kind == ENTRY_CREATE) {
                    handleCreate(fullPath);
                } else if (kind == ENTRY_DELETE) {
                    handleDelete(fullPath);
                } else if (kind == ENTRY_MODIFY) {
                    handleModify(fullPath);
                }
            } catch (Exception e) {
                logger.warn("Failed to process event for: {}", fullPath, e);
            }
        }

        key.reset();
    }

    /**
     * 处理创建事件
     */
    private void handleCreate(Path fullPath) {
        // 检查是否为新目录
        if (Files.isDirectory(fullPath) && Files.exists(fullPath.resolve("SKILL.md"))) {
            String skillName = fullPath.getFileName().toString();

            synchronized (skillsMap) {
                if (!skillsMap.containsKey(skillName)) {
                    Skill skill = loadSkillSafely(fullPath);
                    if (skill != null) {
                        skillsMap.put(skillName, baseDir.relativize(fullPath));
                        updater.upsert(skill).toCompletableFuture().join();
                        logger.info("Loaded new skill: {}", skillName);
                    }
                }
            }
        }
        // 递归注册新目录
        if (Files.isDirectory(fullPath)) {
            try {
                registerDirectory(fullPath);
            } catch (IOException e) {
                logger.warn("Failed to register new directory: {}", fullPath, e);
            }
        }
    }

    /**
     * 处理删除事件
     */
    private void handleDelete(Path fullPath) {
        String skillName = fullPath.getFileName().toString();

        synchronized (skillsMap) {
            if (skillsMap.containsKey(skillName)) {
                skillsMap.remove(skillName);
                updater.remove(skillName).toCompletableFuture().join();
                logger.info("Removed skill: {}", skillName);
            }
        }
    }

    /**
     * 处理修改事件
     */
    private void handleModify(Path fullPath) {
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
    }

    /**
     * 初始化失败时清理资源
     */
    private void cleanupOnFailure() {
        try {
            if (watchService != null) {
                watchService.close();
            }
        } catch (IOException e) {
            logger.warn("Failed to close watch service", e);
        }
    }

    @Override
    public void close() {
        // Signal shutdown
        if (!closeF.complete(null)) {
            return;
        }

        logger.debug("{} closing...", this);

        // Remove all Skills
        if (updater != null) {
            List.copyOf(skillsMap.keySet()).forEach(name -> {
                updater.remove(name);
            });
            updater = null;
        }

        // Stop watcher thread
        watcher.interrupt();
        try {
            watcher.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Close watch service
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.warn("Failed to close watch service", e);
            }
        }

        // Clear storage
        synchronized (skillsMap) {
            skillsMap.clear();
        }

        logger.debug("{} closed", this);
    }

    /**
     * Builder 类 - 用于构造 FileSkillProvider
     */
    public static class Builder {

        private Path baseDir;
        private boolean blocking = true;

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
