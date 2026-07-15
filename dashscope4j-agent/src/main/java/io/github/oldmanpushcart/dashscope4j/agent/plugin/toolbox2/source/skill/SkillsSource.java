package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.skill;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.AbstractToolSource;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class SkillsSource extends AbstractToolSource {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final List<Path> directories;
    private final Duration scanInterval;
    private final boolean isOwnScheduler;
    private final boolean blockingInitialize;
    private final String _toString;

    private final Map<Path, Skill> snapshots = new ConcurrentHashMap<>();

    private volatile State state = State.IDLE;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduleF;

    private SkillsSource(Builder builder) {
        super(builder.name);
        this.directories = CommonUtils.unmodifiableCopy(builder.directories);
        this.scanInterval = builder.scanInterval;
        this.scheduler = builder.scheduler;
        this.isOwnScheduler = Objects.isNull(this.scheduler);
        this.blockingInitialize = builder.blockingInitialize;
        this._toString = "dashscope4j-agent:/toolbox/source/skills/%s".formatted(name());
    }

    @Override
    public String toString() {
        return _toString;
    }

    @Override
    public List<Tool> tools() {

        if (State.RUNNING != state) {
            throw new IllegalStateException("Not initialized!");
        }

        return snapshots.values()
                .stream()
                .map(SkillFunction::new)
                .map(SkillFunction::asTool)
                .toList();
    }

    @Override
    public synchronized SkillsSource initialize() {

        if (State.CLOSED == state) {
            throw new IllegalStateException("Already closed!");
        }

        if (State.RUNNING == state) {
            return this;
        }

        // 初始化扫描线程
        if (isOwnScheduler) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                final var t = new Thread(r, "%s/scanner".formatted(SkillsSource.this));
                t.setDaemon(true);
                return t;
            });
        }

        // 启动扫描任务
        scheduleF = scheduler.scheduleAtFixedRate(
                () -> {

                    // 扫描发现变更，则发送变更事件
                    try {
                        if (scanning()) {
                            fireChanged();
                        }
                    } catch (Throwable t) {
                        logger.warn("{} scanning failed by error!", this, t);
                    }

                },
                scanInterval.toMillis(),
                scanInterval.toMillis(),
                TimeUnit.MILLISECONDS
        );

        // 强制扫描
        if (blockingInitialize) {
            scanning();
        }


        // 状态流转为运行中
        state = State.RUNNING;
        logger.debug("{} initialized.", this);

        return this;
    }

    private synchronized boolean scanning() {

        // 当前版本快照
        final var currentVersions = directories.stream()
                .filter(Files::exists)
                .filter(Files::isDirectory)

                // 找出所有符合规范的SKILL目录
                .flatMap(directory -> {
                    try {
                        return Files.find(directory, 1, (path, attrs) -> attrs.isDirectory());
                    } catch (IOException e) {
                        logger.debug("{}/scanning ignored skills directory by error! directory: {}", this, directory, e);
                        return Stream.empty();
                    }
                })

                // 读取所有符合规范SKILL目录下，SKILL.md文件的修改时间戳
                .map(path -> {
                    final var home = path.resolve("SKILL.md");
                    if (!Files.exists(home)) {
                        logger.debug("{}/scanning ignored skill directory by SKILL.md not found! path={}", this, path);
                        return null;
                    }
                    try {
                        final var lastModifiedAt = Files.getLastModifiedTime(home).toInstant();
                        return Map.entry(path, lastModifiedAt);
                    } catch (IOException e) {
                        logger.debug("{}/scanning ignored skill directory by error! path={}", this, path, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)

                // 转换为当前版本快照
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


        // 找出等待删除的集合
        final var toBeRemovePaths = snapshots.keySet()
                .stream()
                .filter(path -> !currentVersions.containsKey(path))
                .collect(Collectors.toSet());

        // 找出快照版本变更的集合
        final var toBeUpsertPaths = snapshots.values()
                .stream()
                .filter(snapshot -> {
                    final var version = currentVersions.get(snapshot.home());
                    return null == version
                            || !Objects.equals(version, snapshot.lastModifiedAt());
                })
                .map(Skill::home)
                .collect(Collectors.toSet());

        // 先删除所有有变动的
        Stream.of(toBeRemovePaths, toBeUpsertPaths)
                .flatMap(Collection::stream)
                .forEach(path -> {
                    final var skill = snapshots.remove(path);
                    if (null != skill) {
                        logger.debug("{}/scanning remove skill. name={};home={};", this, skill.header().name(), skill.home());
                    }
                });

        // 再添加变更的
        toBeUpsertPaths
                .stream()
                .map(home -> {
                    try {
                        final var skill = Skill.of(home);
                        logger.debug("{}/scanning upsert skill. name={};home={};", this, skill.header().name(), skill.home());
                        return skill;
                    } catch (IOException e) {
                        logger.warn("{}/scanning ignored skill by error! home={}", this, home, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .forEach(skill -> snapshots.put(skill.home(), skill));

        // 通知外边，扫描发现了变动
        return !toBeUpsertPaths.isEmpty() || !toBeRemovePaths.isEmpty();
    }

    @Override
    public boolean isClosed() {
        return State.CLOSED == state;
    }

    @Override
    public synchronized void close() {

        if (State.CLOSED == state) {
            return;
        }

        if (null != scheduleF) {
            scheduleF.cancel(true);
            scheduleF = null;
        }

        if (null != scheduler && isOwnScheduler) {
            scheduler.shutdownNow();
            scheduler = null;
        }

        super.close();
        logger.debug("{} closed.", this);
    }

    private enum State {
        IDLE,
        RUNNING,
        CLOSED
    }

    public static class Builder implements Buildable<SkillsSource, Builder> {

        private String name;
        private List<Path> directories;
        private ScheduledExecutorService scheduler;
        private Duration scanInterval = Duration.ofSeconds(5);
        private boolean blockingInitialize;

        public Builder name(String name) {
            this.name = name;
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

        public Builder scheduler(ScheduledExecutorService scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public Builder scanInterval(Duration scanInterval) {
            this.scanInterval = scanInterval;
            return this;
        }

        public Builder blockingInitialize(boolean blockingInitialize) {
            this.blockingInitialize = blockingInitialize;
            return this;
        }

        @Override
        public SkillsSource build() {
            return new SkillsSource(this);
        }

    }

}
