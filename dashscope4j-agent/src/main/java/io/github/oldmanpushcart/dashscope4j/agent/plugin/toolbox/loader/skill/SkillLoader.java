package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.skill;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.ToolUse;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.AbstractToolLoader;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * Skill 工具加载器
 * <p>
 * 从指定目录加载 Skill，每个子目录代表一个 Skill。
 * 支持文件系统监听，当 SKILL.md 文件变化时自动重新加载。
 * </p>
 */
public class SkillLoader extends AbstractToolLoader {

    private static final Logger logger = LoggerFactory.getLogger(SkillLoader.class);
    private static final String SKILL_MD_FILE = "SKILL.md";

    private final ToolUse.Mode mode;
    private final List<Path> directories;
    private final DirectoryWatcher watcher;

    private final Map<String, Skill> skills = new ConcurrentHashMap<>();
    private final Map<String, ToolUse> uses = new ConcurrentHashMap<>();
    private final CompletableFuture<?> closeF = new CompletableFuture<>();

    private SkillLoader(Builder builder) {
        this.mode = builder.mode;
        this.directories = CommonUtils.unmodifiableCopy(builder.directories);
        this.watcher = new DirectoryWatcher(directories, this::changeSkill);
    }

    @Override
    public String toString() {
        return "dashscoppe4j-agent:/toolbox/loader/skill";
    }

    CompletionStage<SkillLoader> init() {

        // 1. 加载所有技能
        initSkills();

        // 2. 构建初始 ToolUse 集合
        initToolUses();

        // 3. 启动文件监听器
        watcher.start();

        return CompletableFuture.completedFuture(this);
    }

    private void initSkills() {

        directories.forEach(directory -> {

            if (!Files.exists(directory) || !Files.isDirectory(directory)) {
                logger.warn("{} directory not found or not a directory: {}", this, directory);
                return;
            }

            try (final var __stream__ = Files.list(directory)) {
                __stream__
                        .filter(Files::isDirectory)
                        .forEach(skillDir -> {
                            try {
                                final var skill = Skill.of(skillDir);
                                final var name = skill.header().name();
                                skills.put(name, skill);
                                logger.debug("{} loaded {} from {}", this, name, skillDir);
                            } catch (IOException e) {
                                logger.warn("{} skipping invalid skill directory {}", this, skillDir, e);
                            }
                        });
            } catch (IOException e) {
                logger.warn("{} failed to list directory {}", this, directory, e);
            }

        });

    }

    private void initToolUses() {

        // 添加全局工具
        List.of(
                new GetReferenceFunction(skills).asTool(),
                new GetAssetFunction(skills).asTool(),
                new ExecuteScriptFunction(skills, Duration.ofSeconds(30)).asTool()
        ).forEach(tool -> {
            final var name = tool.meta().name();
            final var use = new ToolUse(ToolUse.Mode.FIXED, tool, this);
            uses.put(name, use);
        });

        // 添加技能工具
        skills.values()
                .stream()
                .map(skill -> new LoadSkillFunction(skill).asTool())
                .forEach(tool -> {
                    final var name = tool.meta().name();
                    final var use = new ToolUse(mode, tool, this);
                    uses.put(name, use);
                });

    }

    private void changeSkill(Path skillDir) {
        try {

            final var skill = Skill.of(skillDir);
            final var tool = new LoadSkillFunction(skill).asTool();
            final var use = new ToolUse(ToolUse.Mode.DYNAMIC, tool, this);

            // 更新缓存
            skills.put(skill.header().name(), skill);
            uses.put(tool.meta().name(), use);

            // 计算 upserts/removes
            final List<String> removes = List.of();
            final List<ToolUse> upserts = List.of(use);

            // 通知变更
            notifyChanged(upserts, removes);

        } catch (IOException e) {
            logger.warn("{} failed to reload skill from {}: {}", this, skillDir, e.getMessage());
        }
    }

    @Override
    public List<ToolUse> loaded() {
        return List.copyOf(uses.values());
    }


    @Override
    public void close() {

        // 调用父类 close()，内部会检查 closeF 防重复执行
        super.close();

        if (!closeF.complete(null)) {
            return;
        }

        // 停止监听器
        if (watcher != null) {
            watcher.interrupt();
        }

        // 清空缓存
        uses.clear();
        skills.clear();

    }

    /**
     * 目录监听器（内部类）
     */
    private static class DirectoryWatcher extends Thread {
        private static final Logger logger = LoggerFactory.getLogger(DirectoryWatcher.class);

        private final Consumer<Path> onSkillChanged;
        private final WatchService watchService;

        DirectoryWatcher(List<Path> directories, Consumer<Path> onSkillChanged) {
            setName(toString());
            setDaemon(true);
            this.onSkillChanged = onSkillChanged;

            try {
                this.watchService = FileSystems.getDefault().newWatchService();
                for (Path directory : directories) {
                    if (Files.exists(directory) && Files.isDirectory(directory)) {
                        directory.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Init skill directories watcher occur error!", e);
            }
        }

        @Override
        public String toString() {
            return "dashscope4j-agent:/toolbox/loader/skill/watcher";
        }

        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    try {
                        final var key = watchService.take();
                        for (final var event : key.pollEvents()) {

                            if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                                continue;
                            }

                            final var filename = (Path) event.context();
                            final var parentPath = (Path) key.watchable();
                            final var fullPath = parentPath.resolve(filename);

                            if (isSkillMdFile(fullPath)) {
                                final var skillDir = fullPath.getParent();
                                if (skillDir != null) {
                                    try {
                                        onSkillChanged.accept(skillDir);
                                    } catch (Throwable ex) {
                                        logger.warn("{} failed to process watch! event={};skill={};", this, event, skillDir, ex);
                                    }
                                }
                            }
                        }
                        key.reset();

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        logger.warn("{} failed to process watch event!", this, e);
                    }
                }
            } finally {
                IOUtils.closeQuietly(watchService);
            }
        }

        private boolean isSkillMdFile(Path path) {
            return path != null
                    && SKILL_MD_FILE.equals(path.getFileName().toString())
                    && Files.exists(path);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Builder 模式构造器
     */
    public static class Builder implements Buildable<SkillLoader, Builder> {

        private ToolUse.Mode mode = ToolUse.Mode.DYNAMIC;
        private List<Path> directories = Collections.emptyList();

        public Builder mode(ToolUse.Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder directories(List<Path> directories) {
            this.directories = directories;
            return this;
        }

        public Builder directories(UnaryOperator<List<Path>> operator) {
            this.directories = operator.apply(CommonUtils.mutableCopy(this.directories));
            return this;
        }

        @Override
        public SkillLoader build() {
            return buildAsync()
                    .toCompletableFuture()
                    .join();
        }

        public CompletionStage<SkillLoader> buildAsync() {
            //noinspection resource
            return new SkillLoader(this)
                    .init();
        }
    }

}
