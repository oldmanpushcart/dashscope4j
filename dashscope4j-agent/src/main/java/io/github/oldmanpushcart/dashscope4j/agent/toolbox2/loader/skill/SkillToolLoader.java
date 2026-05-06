package io.github.oldmanpushcart.dashscope4j.agent.toolbox2.loader.skill;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox2.ToolUse;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox2.loader.Bundle;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox2.loader.ChangedListener;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox2.loader.Subscription;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox2.loader.ToolLoader;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Skill 工具加载器
 * <p>
 * 从指定目录加载 Skill，每个子目录代表一个 Skill。
 * 支持文件系统监听，当 SKILL.md 文件变化时自动重新加载。
 * </p>
 */
public class SkillToolLoader implements ToolLoader {

    private static final Logger logger = LoggerFactory.getLogger(SkillToolLoader.class);
    private static final String SKILL_MD_FILE = "SKILL.md";

    private final List<Path> directories;
    private final Map<String, Skill> loadedSkills = new ConcurrentHashMap<>();
    private final List<ChangedListener> listeners = new CopyOnWriteArrayList<>();
    private DirectoryWatcher watcher;

    private SkillToolLoader(Builder builder) {
        this.directories = Collections.unmodifiableList(new ArrayList<>(builder.directories));
        // 在构造函数中启动文件监听器
        this.watcher = new DirectoryWatcher(directories, this::reloadSkill);
        this.watcher.start();
    }

    @Override
    public CompletionStage<Bundle> load() {
        loadedSkills.clear();
        directories.forEach(this::loadSkillsFromDirectory);

        // Skill 加载工具（DYNAMIC 模式）
        final var skillUses = loadedSkills.values().stream()
                .map(this::skillToToolUse)
                .filter(Objects::nonNull)
                .toList();

        // 全局 Skill 辅助工具（FIXED 模式）
        final var globalTools = List.of(
                new ToolUse(ToolUse.Mode.FIXED, new GetReferenceFunction(loadedSkills).asTool()),
                new ToolUse(ToolUse.Mode.FIXED, new GetAssetFunction(loadedSkills).asTool()),
                new ToolUse(ToolUse.Mode.FIXED, new ExecuteScriptFunction(loadedSkills, Duration.ofSeconds(30)).asTool())
        );

        // 合并所有工具
        final var allUses = new ArrayList<ToolUse>();
        allUses.addAll(globalTools);
        allUses.addAll(skillUses);

        return CompletableFuture.completedFuture(new Bundle(allUses, this));
    }

    private void loadSkillsFromDirectory(Path directory) {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            logger.warn("Directory not found or not a directory: {}", directory);
            return;
        }

        try (var stream = Files.list(directory)) {
            stream.filter(Files::isDirectory)
                    .forEach(skillDir -> {
                        try {
                            final var skill = Skill.of(skillDir);
                            loadedSkills.put(skill.header().name(), skill);
                            logger.debug("Loaded skill: {} from {}", skill.header().name(), skillDir);
                        } catch (IOException e) {
                            logger.debug("Skipping invalid skill directory {}: {}", skillDir, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            logger.error("Failed to list directory {}: {}", directory, e.getMessage());
        }
    }

    /**
     * 将 Skill 转换为 ToolUse
     */
    private ToolUse skillToToolUse(Skill skill) {
        final var loadFunction = new LoadSkillFunction(skill);
        return new ToolUse(ToolUse.Mode.DYNAMIC, loadFunction.asTool());
    }

    private void reloadSkill(Path skillDir) {
        try {
            final var skill = Skill.of(skillDir);
            loadedSkills.put(skill.header().name(), skill);
            notifyChanged();
        } catch (IOException e) {
            logger.debug("Failed to reload skill from {}: {}", skillDir, e.getMessage());
        }
    }

    @Override
    public Subscription subscribe(ChangedListener listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    private void notifyChanged() {
        listeners.forEach(listener -> {
            try {
                listener.onChanged(this);
            } catch (Exception e) {
                logger.error("Error notifying listener", e);
            }
        });
    }

    @Override
    public boolean shared() {
        return false;
    }

    @Override
    public void close() {
        if (watcher != null) {
            watcher.interrupt();
            watcher = null;
        }
        listeners.clear();
        loadedSkills.clear();
    }

    /**
     * 目录监听器（内部类）
     */
    private static class DirectoryWatcher extends Thread {
        private static final Logger logger = LoggerFactory.getLogger(DirectoryWatcher.class);

        private final Consumer<Path> onSkillChanged;
        private final WatchService watchService;

        DirectoryWatcher(List<Path> directories, Consumer<Path> onSkillChanged) {
            super("skill-watcher");
            setDaemon(true);
            this.onSkillChanged = onSkillChanged;
            
            try {
                this.watchService = FileSystems.getDefault().newWatchService();
                for (Path directory : directories) {
                    if (Files.exists(directory) && Files.isDirectory(directory)) {
                        directory.register(watchService,
                                StandardWatchEventKinds.ENTRY_CREATE,
                                StandardWatchEventKinds.ENTRY_MODIFY,
                                StandardWatchEventKinds.ENTRY_DELETE);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to initialize watcher", e);
            }
        }

        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    try {
                        final var key = watchService.take();
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                                continue;
                            }

                            final var filename = (Path) event.context();
                            final var parentPath = (Path) key.watchable();
                            final var fullPath = parentPath.resolve(filename);

                            if (isSkillMdFile(fullPath)) {
                                final var skillDir = fullPath.getParent();
                                if (skillDir != null) {
                                    onSkillChanged.accept(skillDir);
                                }
                            }
                        }
                        key.reset();

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        logger.error("Error in watch loop", e);
                    }
                }
            } finally {
                try {
                    watchService.close();
                } catch (IOException e) {
                    logger.error("Failed to close watch service", e);
                }
            }
        }

        private boolean isSkillMdFile(Path path) {
            return path != null
                    && SKILL_MD_FILE.equals(path.getFileName().toString())
                    && Files.exists(path);
        }
    }

    /**
     * Builder 模式构造器
     */
    public static class Builder implements Buildable<SkillToolLoader, Builder> {
        private List<Path> directories = Collections.emptyList();

        public Builder directories(List<Path> directories) {
            this.directories = Objects.requireNonNullElse(directories, Collections.emptyList());
            return this;
        }

        public Builder directories(Path... directories) {
            return directories(Arrays.asList(directories));
        }

        @Override
        public SkillToolLoader build() {
            return new SkillToolLoader(this);
        }
    }

}
