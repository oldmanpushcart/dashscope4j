package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill.provider.file;

import io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.skill.Skill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * 文件系统监控器 - 负责监听文件变化并管理草稿集合
 * <p>
 * 职责：
 * 1. 监听 baseDir 及其子目录的文件变更事件
 * 2. 当检测到 SKILL.md 创建/修改时，加载 Skill 并更新到草稿集合
 * 3. 当检测到目录删除时，从草稿集合移除
 * 4. 持有并管理草稿集合（drafts）
 * </p>
 *
 * @since 4.0.0
 */
class Monitor extends Thread implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(Monitor.class);

    // === Configuration ===
    private final Path baseDir;
    private final String _toString;
    private final ChangeHandler changeHandler;

    // === Shared Data (Managed by Monitor) ===
    // 草稿集合：skillName -> Entry (由 Monitor 直接管理)
    private final Map<String, Entry> drafts = Collections.synchronizedMap(new HashMap<>());

    /**
     * 变更处理器接口 - 用于通知外部草稿集合已变更
     */
    interface ChangeHandler {

        /**
         * 草稿集合变更通知
         *
         * @param entry  技能条目（包含 Skill 和 Path）
         * @param delete 是否为删除操作
         */
        void onDraftChanged(Entry entry, boolean delete);

    }

    /**
     * 创建文件监控器
     *
     * @param baseDir       基础监听目录
     * @param changeHandler 变更处理器
     */
    Monitor(Path baseDir, ChangeHandler changeHandler) {
        super("FileMonitor-%s".formatted(baseDir.getFileName()));
        this.baseDir = baseDir.toAbsolutePath().normalize();
        this.changeHandler = changeHandler;
        this._toString = "dashscope4j-agent:/skill/monitor/%s".formatted(this.baseDir.getFileName());
        this.setDaemon(true);
    }

    /**
     * 获取草稿集合（只读视图）
     *
     * @return 草稿集合的只读视图
     */
    Map<String, Entry> getDrafts() {
        return Collections.unmodifiableMap(drafts);
    }

    @Override
    public String toString() {
        return _toString;
    }

    /**
     * 监听目录及其子目录
     */
    private void watching(WatchService watch, Path dir) throws IOException {

        dir.register(watch, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

        // 递归注册所有子目录
        try (final var paths = Files.list(dir)) {
            paths.filter(Files::isDirectory)
                    .forEach(child -> {
                        try {
                            watching(watch, child);
                        } catch (IOException e) {
                            logger.warn("Failed to register child directory: {}", child, e);
                        }
                    });
        }
    }

    @Override
    public void run() {
        logger.debug("{} started", this);

        try (final var watch = baseDir.getFileSystem().newWatchService()) {

            // 递归监听所有现有目录
            watching(watch, baseDir);

            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key;
                try {
                    key = watch.poll(1, TimeUnit.SECONDS);
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
        } catch (IOException e) {
            logger.error("{} watching occur error", this, e);
        } finally {
            logger.debug("{} thread stopped", this);
        }
    }

    /**
     * 处理 Watch 事件
     */
    private void processEvents(WatchKey key) {

        for (final var event : key.pollEvents()) {

            final var kind = event.kind();

            /*
             * 被监听的根目录
             * 确保 watchable 是 Path 类型
             *
             * 例如：/Users/dev/skills
             */
            if (!(key.watchable() instanceof Path basePath)) {
                continue;
            }

            /*
             * 事件发生的相对路径
             * 确保 context 是 Path 类型
             *
             * 例如：math/add
             */
            if (!(event.context() instanceof Path pathContext)) {
                continue;
            }

            /*
             * 完整路径
             * 由 <根目录>/<事件发生的相对路径> 拼接而成
             *
             * 例如：/Users/dev/skills/math/add
             */
            final var fullPath = basePath.resolve(pathContext);

            try {
                if (kind == ENTRY_CREATE) {
                    handleFileCreate(fullPath);
                } else if (kind == ENTRY_DELETE) {
                    handleFileDelete(fullPath);
                } else if (kind == ENTRY_MODIFY) {
                    handleFileModify(fullPath);
                }
            } catch (Exception e) {
                logger.warn("{} process event:{}, home:{} occur error!", this, event.kind(), fullPath, e);
            }
        }

        key.reset();
    }

    /**
     * 处理文件创建事件
     */
    private void handleFileCreate(Path fullPath) {
        // 检查是否为新技能目录（包含 SKILL.md）
        if (Files.isDirectory(fullPath) && Files.exists(fullPath.resolve("SKILL.md"))) {
            String skillName = fullPath.getFileName().toString();
            logger.debug("Detected new skill directory: {}", skillName);
            loadAndUpdateDraft(skillName, fullPath);
        }
        // 如果是 SKILL.md 文件创建，也触发加载
        else if (fullPath.getFileName().toString().equals("SKILL.md") && Files.isDirectory(fullPath.getParent())) {
            String skillName = fullPath.getParent().getFileName().toString();
            logger.debug("Detected new SKILL.md: {}", skillName);
            loadAndUpdateDraft(skillName, fullPath.getParent());
        }
    }

    /**
     * 处理文件删除事件
     */
    private void handleFileDelete(Path fullPath) {
        // 如果是整个技能目录删除
        if (fullPath.getFileName().toString().equals("SKILL.md") && Files.isDirectory(fullPath.getParent())) {
            String skillName = fullPath.getParent().getFileName().toString();
            logger.debug("Detected skill deletion: {}", skillName);
            removeFromDraft(skillName);
        }
        // 如果是目录删除且该目录是技能目录
        else if (Files.isDirectory(fullPath) && !Files.exists(fullPath.resolve("SKILL.md"))) {
            String skillName = fullPath.getFileName().toString();
            logger.debug("Detected skill directory deletion: {}", skillName);
            removeFromDraft(skillName);
        }
    }

    /**
     * 处理文件修改事件
     */
    private void handleFileModify(Path fullPath) {
        // 如果是 SKILL.md 文件修改
        if (fullPath.getFileName().toString().equals("SKILL.md") && Files.isDirectory(fullPath.getParent())) {
            String skillName = fullPath.getParent().getFileName().toString();
            logger.debug("Detected SKILL.md modification: {}", skillName);
            loadAndUpdateDraft(skillName, fullPath.getParent());
        }
    }

    /**
     * 加载 Skill 并更新草稿集合
     */
    private void loadAndUpdateDraft(String skillName, Path skillDir) {
        try {
            Skill skill = FileSkill.valueOf(skillDir);
            Entry entry = new Entry(skill, skillDir);
            drafts.put(skillName, entry);
            logger.debug("Updated draft: {}", skillName);
            // 通知外部草稿集合已变更（直接传递 Entry）
            changeHandler.onDraftChanged(entry, false);
        } catch (Exception e) {
            logger.warn("Failed to load skill from: {}", skillDir, e);
        }
    }

    /**
     * 从草稿集合移除
     */
    private void removeFromDraft(String skillName) {
        Entry removedEntry = drafts.remove(skillName);
        logger.debug("Removed from draft: {}", skillName);
        // 通知外部草稿集合已变更（传递被删除的 Entry）
        if (removedEntry != null) {
            changeHandler.onDraftChanged(removedEntry, true);
        }
    }

    @Override
    public void close() {
        this.interrupt();
    }
}
